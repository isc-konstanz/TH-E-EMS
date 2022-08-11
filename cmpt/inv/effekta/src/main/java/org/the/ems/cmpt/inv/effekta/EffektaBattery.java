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
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

public class EffektaBattery extends ElectricalEnergyStorage {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	private final static String SECTION = "EnergyStorage";

	private final static int CURRENT_MAX = 200;

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

	@Configuration(value = "*_power*", mandatory = false)
	private ChannelCollection externalPowers;

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

	private long modeChangedTime = Long.MIN_VALUE;

	private Mode modeSetting = Mode.DEFAULT;

	private volatile long timestampLast = Long.MIN_VALUE;


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
		long timestamp = power.getEpochMillis();
		try {
			if (power.doubleValue() == 0.) {
				timestamp += onSetMode(container, Mode.DEFAULT, timestamp);
				container.addDouble(chargeCurrentMax, CURRENT_MAX, timestamp);
				container.addDouble(dischargeCurrentMax, CURRENT_MAX, timestamp);
			}
			else {
				double voltage = getVoltage().doubleValue();
				double currentSetpoint = Math.abs(power.doubleValue()/voltage);
				if (currentSetpoint > CURRENT_MAX) {
					currentSetpoint = CURRENT_MAX;
				}
				if (power.doubleValue() >= 0.) {
					if (!isChargable()) {
						throw new ComponentException("Unable to charge battery");
					}
					if (getCurrent().doubleValue() >= currentSetpoint) {
						timestamp += onSetMode(container, Mode.DEFAULT, timestamp);
					}
					else {
						timestamp += onSetMode(container, Mode.CHARGE_FROM_GRID, timestamp);
					}
					container.addDouble(chargeCurrentMax, CURRENT_MAX, timestamp);
					container.addDouble(dischargeCurrentMax, currentSetpoint, timestamp);
				}
				else {
					if (!isDischargable()) {
						throw new ComponentException("Unable to discharge battery");
					}
					timestamp += onSetMode(container, Mode.FEED_INTO_GRID, timestamp);
					container.addDouble(chargeCurrentMax, CURRENT_MAX, timestamp);
					container.addDouble(dischargeCurrentMax, currentSetpoint, timestamp);
				}
			}
			powerSetpoint = power;
			
		} catch (InvalidValueException e) {
			throw new ComponentException(e);
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
		if (onSetVoltageSetpoint(container, mode, timestamp)) {
			timestamp += modeChangeDelay;
		}
		int modeInverse = this.mode.getLatestValue().intValue() & 0xFFFF;
		int modeSetpoint = mode.getInteger();
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
				container.addInteger(this.mode, bits, timestamp);
				timestamp += modeChangeDelay;
				
				logger.debug("Writing battery mode bit: {}", Mode.toBinaryString(bits));
			}
			bitmask = Integer.rotateLeft(bitmask, 1);
		}
		
		this.modeChangedTime = timestamp;
		this.modeSetting = mode;
		return timestamp;
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

	private boolean onSetVoltageSetpoint(WriteContainer container, Mode mode, long timestamp) {
		try {
			if (mode == Mode.FEED_INTO_GRID) {
				if (getVoltageSetpoint().doubleValue() != getVoltageMin().doubleValue()) {
					container.addDouble(voltageSetpoint, getVoltageMin().doubleValue(), timestamp);
					container.addInteger(voltageSetpointEnabled, 1, timestamp);
					return true;
				}
			}
			else {
				if (getVoltageSetpoint().doubleValue() != getVoltageFloating().doubleValue()) {
					container.addDouble(voltageSetpoint, getVoltageFloating().doubleValue(), timestamp);
					container.addInteger(voltageSetpointEnabled, 0, timestamp);
					return true;
				}
			}
		} catch (InvalidValueException e) {
			logger.warn("Error retrieving floating voltage: {}", e.getMessage());
		}
		return false;
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

	public Value getCurrent() throws InvalidValueException {
		return current.getLatestValue();
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
				(getVoltageMax().doubleValue() - getVoltageMin().doubleValue())*100.;
		setStateOfCharge(state, timestamp);
	}

	private void processStateOfCharge(long timestamp) throws InvalidValueException {
		double voltage = getVoltage().doubleValue();
		double current = getCurrent().doubleValue();
		double power = current*voltage;
		setPower(power, timestamp);
		
		power += dischargePowerStandby;	
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
			else if (voltage >= getVoltageMax().doubleValue()) {
				state = 100;
			}
			if (Double.isNaN(current)) {
				return;
			}
			else if (!Double.isNaN(current)) {
				long timestampDelta = timestamp - this.timestampLast;
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
			timestampLast = timestamp;
		}
	}

	private class StateOfChargeListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			double state = value.doubleValue();
			if ((!isChargable(state) || !isDischargable(state)) && powerSetpoint.doubleValue() != 0) {
				try {
					set(DoubleValue.zeroValue());
					
				} catch (EnergyManagementException e) {
					logger.warn("Error while resetting battery setpoint due to state of charge threshold violation: {}",
							e.getMessage());
				}
			}
		}
	}

}
