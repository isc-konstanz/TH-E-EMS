/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.cmpt.inv.effekta;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

public class EffektaBattery extends ElectricalEnergyStorage {
	private final static Logger logger = LoggerFactory.getLogger(EffektaBattery.class);

	private final static String SECTION = "EnergyStorage";

	private final static int CURRENT_MAX = 200;
	private final static int CURRENT_MIN = 10;

	@Configuration
	private Channel voltage;

	@Configuration
	private Channel voltageMax;

	@Configuration
	private Channel voltageMin;

	@Configuration
	private Channel voltageFloating;

	@Configuration
	private Channel voltageSetpoint;

	@Configuration
	private Channel voltageSetpointEnabled;

	@Configuration
	private Channel current;

	@Configuration("current_charge_max")
	private Channel chargeCurrentMax;

	@Configuration("current_discharge_max")
	private Channel dischargeCurrentMax;

	@Configuration(value = "power_standby", mandatory = false)
	private double dischargePowerStandby = 100;

	@Configuration(value = "power_extern_*", mandatory = false)
	private ChannelCollection externalPowers;

	@Configuration(value = "power_input_*")
	private ChannelCollection inputPowers;

	volatile Value powerSetpoint;

	@Configuration
	private Channel power;

	@Configuration
	private Channel soc;

	@Configuration
	private Channel mode;

	@Configuration(mandatory = false)
	private int modeChangeDelay = 3000;

	@Configuration(mandatory = false, scale = 1000)
	private int modeChangedPause = 30000;

	private Mode modeSetting = Mode.DEFAULT;

	private volatile long modeChangedTime = Long.MIN_VALUE;
	private volatile long stateProcessedLast = Long.MIN_VALUE;


	protected EffektaBattery() {
		super(SECTION);
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		try {
			initStateOfCharge(System.currentTimeMillis());
			
		} catch (InvalidValueException e) {
			// Do nothing
		}
		try {
			set(DoubleValue.zeroValue());
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving battery mode: {}", e.getLocalizedMessage());
			
		} catch (EnergyManagementException e) {
			logger.warn("Error setting battery mode: {}", e.getLocalizedMessage());
		}
		soc.registerValueListener(new StateOfChargeListener());
		current.registerValueListener(new ValueListener() {

			@Override
			public void onValueReceived(Value value) {
				try {
					processStateOfCharge(value.getEpochMillis());
					
				} catch (InvalidValueException e) {
					logger.debug("Error processing battery state of charge: {}", e.getLocalizedMessage());
				}
			}
		});
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		soc.deregisterValueListeners();
		voltage.deregisterValueListeners();
	}

	@Override
	protected void onInterrupt() throws ComponentException {
		super.onInterrupt();

		long timestamp = System.currentTimeMillis();
		try {
			if (timestamp - modeChangedTime > modeChangedPause && !modeSetting.equals(mode.getLatestValue())) {
				setMode(modeSetting);
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving battery mode: {}", e.getLocalizedMessage());
			
		} catch (EnergyManagementException e) {
			logger.warn("Error setting battery mode: {}", e.getLocalizedMessage());
		}
	}

	@Override
    protected void onSet(WriteContainer container, Value power) throws ComponentException {
		if (powerSetpoint == null || 
				powerSetpoint.doubleValue() != power.doubleValue()) {
			
			doSetpointChanged(container, power);
			powerSetpoint = power;
		}
    }

	void doSetpointChanged(WriteContainer container, Value power) 
			throws ComponentException {
		try {
			if (!isReady()) {
				throw new ComponentException("Unable to write setpoint while battery mode not applied");
			}
			onSetpointChanged(container, power);
			
		} catch (InvalidValueException e) {
			throw new ComponentException(e);
		}
	}

	protected void onSetpointChanged(WriteContainer container, Value power) 
			throws ComponentException, InvalidValueException {
		
		logger.debug("Setting battery power setpoint: {}", power);
		
		long timestamp = power.getEpochMillis();
		if (power.doubleValue() == 0.) {
			timestamp = onSetMode(container, Mode.DEFAULT, timestamp);
			container.addDoubleIfChanged(chargeCurrentMax, CURRENT_MAX);
			container.addDoubleIfChanged(dischargeCurrentMax, CURRENT_MIN);
		}
		else {
			double voltage = getVoltage().doubleValue();
			double currentSetpoint = Math.abs(power.doubleValue()/voltage);
			if (currentSetpoint > CURRENT_MAX) {
				currentSetpoint = CURRENT_MAX;
			}
			logger.debug("Setting battery charge current limit: {}", currentSetpoint);
			
			if (power.doubleValue() >= 0.) {
				if (!isChargable()) {
					throw new ComponentException("Unable to charge battery");
				}
				double inputCurrentValue = 0;
				for (Channel inputPower : inputPowers.values()) {
					inputCurrentValue = inputPower.getLatestValue().doubleValue()/voltage;
				}
				if (inputCurrentValue < currentSetpoint) {
					timestamp = onSetMode(container, Mode.CHARGE_FROM_GRID, timestamp);
				}
				else {
					timestamp = onSetMode(container, Mode.DEFAULT, timestamp);
				}
				container.addDoubleIfChanged(chargeCurrentMax, currentSetpoint);
				container.addDoubleIfChanged(dischargeCurrentMax, CURRENT_MIN);
			}
			else {
				if (!isDischargable()) {
					throw new ComponentException("Unable to discharge battery");
				}
				timestamp = onSetMode(container, Mode.FEED_INTO_GRID, timestamp);
				container.addDoubleIfChanged(chargeCurrentMax, CURRENT_MAX);
				container.addDoubleIfChanged(dischargeCurrentMax, currentSetpoint);
			}
		}
		if (logger.isDebugEnabled()) {
			for (Entry<Channel, ValueList> channel : container.entrySet()) {
				for (Value value : channel.getValue()) {
					if (mode == channel.getKey()) {
						logger.debug("Writing battery mode bit: {}", Mode.toBinaryString(value.intValue()));
					}
					else {
						logger.debug("Writing value to channel \"{}\": {}", channel.getKey().getId(), value);
					}
				}
			}
		}
	}

	public final void setMode(Mode mode) throws EnergyManagementException {
		doSetMode(mode, System.currentTimeMillis());
	}

	void doSetMode(Mode mode, long timestamp) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		WriteContainer container = new WriteContainer();
		onSetMode(container, mode, timestamp);
		write(container);
	}

	protected long onSetMode(WriteContainer container, Mode mode, long timestamp) throws InvalidValueException {
		long modeChangedTimestamp = timestamp;
		int modeSetpoint = mode.getInteger();
		int modeInverse = this.mode.getLatestValue().intValue() & 0xFFFF;
		int errormask = modeSetpoint ^ modeInverse;
		int bitmask = 0x0100;
		for (int i = 1; i <= 8; i++) {
			if ((errormask & bitmask) != 0) {
				int bits;
				if ((modeSetpoint & bitmask) != 0) {
					bits = bitmask;
				}
				else {
					bits = (~bitmask) & 0xFFFF;
				}
				container.addInteger(this.mode, bits, modeChangedTimestamp);
				modeChangedTimestamp += modeChangeDelay;
			}
			bitmask = Integer.rotateLeft(bitmask, 1);
		}
		modeChangedTimestamp = onSetVoltageSetpoint(container, mode, modeChangedTimestamp);
		
		this.modeSetting = mode;
		this.modeChangedTime = timestamp;
		return modeChangedTimestamp;
	}

	public boolean isReady() throws InvalidValueException {
		return modeChangedTime <= 0 ||
				modeChangedPause < System.currentTimeMillis() - modeChangedTime || 
				modeSetting.equals(mode.getLatestValue()) && hasVoltageSetpoint(modeSetting);
	}

	@Override
	public Value getVoltage() throws InvalidValueException {
		return voltage.getLatestValue();
	}

	@Override
	public void registerVoltageListener(ValueListener listener) {
		voltage.registerValueListener(listener);
	}

	@Override
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException {
		voltage.deregisterValueListener(listener);
	}

	public final void setVoltageSetpoint(Mode mode) throws EnergyManagementException {
		doSetVoltageSetpoint(mode, System.currentTimeMillis());
	}

	void doSetVoltageSetpoint(Mode mode, long timestamp) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		WriteContainer container = new WriteContainer();
		onSetVoltageSetpoint(container, mode, timestamp);
		write(container);
	}

	private long onSetVoltageSetpoint(WriteContainer container, Mode mode, long timestamp) {
		try {
			if (mode == Mode.FEED_INTO_GRID) {
				if (!getVoltageSetpointEnabled().booleanValue()) {
					container.addInteger(voltageSetpointEnabled, 1, timestamp);
					timestamp += modeChangeDelay;
				}
				if (getVoltageSetpoint().doubleValue() != getVoltageMin().doubleValue()) {
					container.addDouble(voltageSetpoint, getVoltageMin().doubleValue(), timestamp);
					timestamp += modeChangeDelay;
				}
			}
			else {
				if (getVoltageSetpoint().doubleValue() != getVoltageFloating().doubleValue()) {
					container.addDouble(voltageSetpoint, getVoltageFloating().doubleValue(), timestamp);
					timestamp += modeChangeDelay;
				}
				if (getVoltageSetpointEnabled().booleanValue()) {
					container.addInteger(voltageSetpointEnabled, 0, timestamp);
					timestamp += modeChangeDelay;
				}
			}
		} catch (InvalidValueException e) {
			logger.warn("Error retrieving floating voltage: {}", e.getMessage());
		}
		return timestamp;
	}

	private boolean hasVoltageSetpoint(Mode mode) throws InvalidValueException {
		if (mode == Mode.FEED_INTO_GRID) {
			return getVoltageSetpointEnabled().booleanValue() && 
					getVoltageSetpoint().doubleValue() == getVoltageMin().doubleValue();
		}
		else {
			return !getVoltageSetpointEnabled().booleanValue() && 
					getVoltageSetpoint().doubleValue() == getVoltageFloating().doubleValue();
		}
	}

	public Value getVoltageMax() throws InvalidValueException {
		return voltageMax.getLatestValue();
	}

	public Value getVoltageMin() throws InvalidValueException {
		return voltageMin.getLatestValue();
	}

	public Value getVoltageFloating() throws InvalidValueException {
		return voltageFloating.getLatestValue();
	}

	public Value getVoltageSetpoint() throws InvalidValueException {
		return voltageSetpoint.getLatestValue();
	}

	public Value getVoltageSetpointEnabled() throws InvalidValueException {
		return voltageSetpointEnabled.getLatestValue();
	}

	public Value getCurrent() throws InvalidValueException {
		return current.getLatestValue();
	}

	public Value getPowerSetpoint() {
		return powerSetpoint;
	}

	public double getPowerMin() throws InvalidValueException {
		return getVoltage().doubleValue()*CURRENT_MIN;
	}

	@Override
	public Value getPower() throws InvalidValueException {
		return power.getLatestValue();
	}

	@Override
	public Value getPower(ValueListener listener) throws InvalidValueException {
		return power.getLatestValue(listener);
	}

	@Override
	public void registerPowerListener(ValueListener listener) throws ComponentException {
		power.registerValueListener(listener);
	}

	@Override
	public void deregisterPowerListener(ValueListener listener) throws ComponentException {
		power.deregisterValueListener(listener);
	}

	private void setPower(double value, long timestamp) {
		power.setLatestValue(new DoubleValue(value, timestamp));
	}

	@Override
	public Value getStateOfCharge() throws InvalidValueException {
		return soc.getLatestValue();
	}

	@Override
	public Value getStateOfCharge(ValueListener listener) throws ComponentException, InvalidValueException {
		return soc.getLatestValue(listener);
	}

	@Override
	public void registerStateOfChargeListener(ValueListener listener) throws ComponentException {
		soc.registerValueListener(listener);
	}

	@Override
	public void deregisterStateOfChargeListener(ValueListener listener) throws ComponentException {
		soc.deregisterValueListener(listener);
	}

	private void setStateOfCharge(double value, long timestamp) {
		if (value < 0.) {
			value = 0.;
		}
		if (value > 100.) {
			value = 100.;
		}
		soc.setLatestValue(new DoubleValue(value, timestamp));
	}

	private void initStateOfCharge(long timestamp) throws InvalidValueException {
		double state = (getVoltage().doubleValue() - getVoltageMin().doubleValue()) / 
				(getVoltageFloating().doubleValue() - getVoltageMin().doubleValue())*100.;
		setStateOfCharge(state, timestamp);
	}

	private void processStateOfCharge(long timestamp) throws InvalidValueException {
		double voltage = getVoltage().doubleValue();
		double current = getCurrent().doubleValue();
		double power = current*voltage;
		setPower(power, timestamp);
		
		power -= dischargePowerStandby;	
		for (Channel externalPower : externalPowers.values()) {
			try {
				double externalPowerValue = externalPower.getLatestValue().doubleValue();
				current -= externalPowerValue/voltage;
				power -= externalPowerValue;
				
			} catch (InvalidValueException e) {
				logger.debug("Error retrieving external power to process current: {}", e);
			}
		}
		try {
			double state = Double.NaN;
			double stateLast = getStateOfCharge().doubleValue();
			if (voltage <= getVoltageMin().doubleValue()) {
				state = 0;
			}
			else if (voltage >= getVoltageFloating().doubleValue()) {
				state = 100;
			}
			if (Double.isNaN(current)) {
				return;
			}
			else if (!Double.isNaN(current)) {
				long timestampDelta = timestamp - this.stateProcessedLast;
				if (timestampDelta < 1000) {
					// Skip too short intervals to avoid artefacts
					return;
				}
				double energy = power/1000.*(timestampDelta/3600000.);
				state = stateLast + energy/getCapacity()*100.;
			}
			if (Double.isNaN(state)) {
				return;
			}
			setStateOfCharge(state, timestamp);
			
		} catch (InvalidValueException e) {
			initStateOfCharge(timestamp);
			
		} finally {
			stateProcessedLast = timestamp;
		}
	}

	private class StateOfChargeListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			double state = value.doubleValue();
			if ((powerSetpoint.doubleValue() > 0 && !isChargable(state)) || 
					(powerSetpoint.doubleValue() < 0 && !isDischargable(state))) {
				try {
					set(DoubleValue.zeroValue());
					
				} catch (EnergyManagementException e) {
					logger.warn("Error resetting battery setpoint due to state of charge threshold violation: {}",
							e.getMessage());
				}
			}
		}
	}

}
