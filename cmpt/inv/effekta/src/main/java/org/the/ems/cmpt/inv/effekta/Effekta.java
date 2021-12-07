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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(scope = ServiceScope.BUNDLE, service = InverterService.class, configurationPid = InverterService.PID
		+ ".effekta", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Effekta extends Inverter<EffektaBattery> {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);
	protected Mode mode = Mode.DISABLED;
//	private Value setpointPower = DoubleValue.emptyValue();
	private double voltage = 0;
	private double current = 0;
	private boolean initialized = false;
	private boolean dischargeProtection = false;
	private boolean overchargeProtection = false;
	private String[] keys;
	private long modeChangedTime = 0;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

	@Configuration(mandatory = false, scale = 1000)
	protected long modeChangedPause = 30000;

	@Configuration
	private Channel operationMode;

	@Configuration
	private Channel setpointCurrentExport;

	@Configuration
	private Channel setpointCurrentImport;

	@Configuration(value = "pwr_*")
	protected ChannelCollection powers;

	@Configuration
	protected ChannelListener currentInv;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerVoltageListener(new VoltageListener());
		currentInv.registerValueListener(new CurrentListener());

		storage.initialize();
		initialize();
	}

	public void initialize() {
		WriteContainer container = new WriteContainer();
		long time = System.currentTimeMillis();

		container.addDouble(storage.getCurrentMax(), Math.min((int) getMaxPower() / storage.getVoltageMin(), 200),
				time);
		setpoint.setLatestValue(new DoubleValue(0, time));
		modeChangedTime = time;

		try {
			doWrite(container);
		} catch (EnergyManagementException e) {
			logger.warn("Effekta activation error: {}", e.getMessage());
		}

		keys = new String[powers.size()];
		int i = powers.size() - 1;
		for (String key : powers.keySet()) {
			keys[i] = key;
			powers.get(key).setLatestValue(new DoubleValue(0));
			i--;
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
//		if (System.currentTimeMillis() >= setpointPower.getTime() + 15000) {
//			
//		}
//		if (time >= modeChangedPause + modeChangedTime) {

		try {
			if (value.doubleValue() == 0 && mode != Mode.DISABLED) {
				setMode(container, time, Mode.DISABLED);
			} else if (value.doubleValue() < 0 && mode != Mode.CHARGE_FROM_GRID && !overchargeProtection) {
				setMode(container, time, Mode.CHARGE_FROM_GRID);
			} else if (value.doubleValue() > 0 && mode != Mode.FEED_INTO_GRID && !dischargeProtection) {
				setMode(container, time, Mode.FEED_INTO_GRID);
			}
		} catch (InvalidValueException e) {
			logger.debug("Invalid value exception error: {}", e.getMessage());
		}

		switch (mode) {
		case CHARGE_FROM_GRID:
			if (!overchargeProtection) {
				container.addDouble(setpointCurrentImport, -value.doubleValue() / voltage, time);
				setpoint.setLatestValue(value);
			} else {
				container.addDouble(setpointCurrentImport, 0, time);
				setpoint.setLatestValue(new DoubleValue(0, time));
			}
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					if (!dischargeProtection) {
						if (overchargeProtection && powers.containsKey("pwr_fuelcell")
								&& powers.get("pwr_fuelcell").getLatestValue().doubleValue() > 0) {
							value = new DoubleValue(powers.get("pwr_fuelcell").getLatestValue().doubleValue(), time);
						}
						container.addDouble(setpointCurrentExport, value.doubleValue() / voltage, time);
						setpoint.setLatestValue(value);
					} else {
						container.addDouble(setpointCurrentExport, 0, time);
						setpoint.setLatestValue(new DoubleValue(0, time));
					}
				}
			} catch (Exception e) {
				logger.warn("Obligatory value missing on setpoint update: {}", e.getMessage());
			}
			break;
		case DEFAULT:
		case DISABLED:
			break;
		}
//		}
	}

	@Override
	protected void onInterrupt() throws ComponentException {
		// TODO: Verify if power setpoint is actually reached
		long time = System.currentTimeMillis();

		if (time >= modeChangedTime + modeChangedPause) {
			try {
				WriteContainer container = new WriteContainer();

				// check if setpoint power is actually reached (tolerance = 20%)
				if (hasReachedSetpoint(container, time, 0.2)) {
					onSetpointChanged(container, new DoubleValue(setpoint.getLatestValue().doubleValue(), time));
				}

				// check if voltage setpoint is set
				setVoltageSetpoint(container, time);

				// check if mode is actually reached
				if (operationMode.getLatestValue().longValue() != (mode.getLong() & 0xFFFFL)) {
					setMode(container, time, mode);
				}

				doWrite(container);

			} catch (EnergyManagementException e) {
				modeChangedTime = time;
				logger.warn("Interrupt call failed: {}", e.getLocalizedMessage());
			}
		}
	}

	public boolean hasReachedSetpoint(WriteContainer container, long time, double tolerance)
			throws InvalidValueException, ComponentException {
		if (powers.get("pwr_inverter").getLatestValue().doubleValue() >= setpoint.getLatestValue().doubleValue()
				* (1 + tolerance)
				|| powers.get("pwr_inverter").getLatestValue().doubleValue() <= setpoint.getLatestValue().doubleValue()
						* (1 - tolerance)) {
			return false;
		} else {
			return true;
		}
	}

	public boolean setVoltageSetpoint(WriteContainer container, long timestamp) {
		try {
			if (mode == Mode.CHARGE_FROM_GRID) {
				if (storage.getVoltageSetpoint().getLatestValue().doubleValue() != storage.getVoltageMax()) {
					container.addDouble(storage.getVoltageSetpoint(), storage.getVoltageMax(), timestamp);
					return true;
				}
			} else if (mode == Mode.FEED_INTO_GRID) {
				if (storage.getVoltageSetpoint().getLatestValue().doubleValue() != storage.getVoltageMin()) {
					container.addDouble(storage.getVoltageSetpoint(), storage.getVoltageMin(), timestamp);
					return true;
				}
			}
		} catch (InvalidValueException e) {
			logger.warn("No voltage setpoint available {}", e.getMessage());
		}
		return false;
	}

	private void setMode(WriteContainer container, long timestamp, Mode mode) throws InvalidValueException {
		long actualMode = operationMode.getLatestValue().shortValue() & 0xFFFF;
		long modeSetpoint = mode.getLong() & 0xFFFF;
		long errormask = modeSetpoint ^ actualMode;
		long bitmask = 0x0001L;
		long nBitmask;

		if (setVoltageSetpoint(container, timestamp)) {
			timestamp += messagesDelay;
		}

		for (int i = 1; i <= 16; i++) {
			if ((errormask & bitmask) != 0) {
				if ((modeSetpoint & bitmask) != 0) {
					container.addDouble(operationMode, bitmask, timestamp);
					timestamp += messagesDelay;
				} else {
					nBitmask = (~bitmask) & 0xFFFF;
					container.addDouble(operationMode, nBitmask, timestamp);
					timestamp += messagesDelay;
				}
			}
			bitmask = Long.rotateLeft(bitmask, 1);
		}

		logger.info("Mode: {} - setting bits:", mode);
		for (ValueList values : container.values()) {
			for (Value value : values) {
				logger.info("Bits: {}", Long.toBinaryString((int) value.doubleValue()));
			}
		}
		logger.info(" --------------- ");

		modeChangedTime = System.currentTimeMillis();
		this.mode = mode;
	}

	public void getDischargeProtection(Long time)
			throws InvalidValueException, UnsupportedOperationException, EnergyManagementException {
		if (storage.getStateOfCharge().doubleValue() <= storage.getMinStateOfCharge() && !dischargeProtection) {
			dischargeProtection = true;
			set(new DoubleValue(0, time));
			logger.warn("Low SOC! Feeding into the Grid stopped.");
		} else if (storage.getStateOfCharge().doubleValue() > storage.getMinStateOfCharge() + 5
				&& dischargeProtection) {
			dischargeProtection = false;
		}
	}

	public void getOverchargeProtection()
			throws InvalidValueException, UnsupportedOperationException, EnergyManagementException {
		if (storage.getStateOfCharge().doubleValue() >= storage.getMaxStateOfCharge() && !overchargeProtection) {
			overchargeProtection = true;
		} else if (storage.getStateOfCharge().doubleValue() < storage.getMaxStateOfCharge() - 2
				&& overchargeProtection) {
			overchargeProtection = false;
		}
	}

	private void setStoragePower() throws InvalidValueException {
		Double pwr = current * voltage;

		powers.get("pwr_inverter").setLatestValue(new DoubleValue(current * voltage));
		for (String key : keys) {
			switch (key) {
			case "pwr_photovoltaik":
				pwr -= powers.get(key).getLatestValue().doubleValue();
				break;
			case "pwr_fuelcell":
				pwr -= powers.get(key).getLatestValue().doubleValue();
				break;
			}
		}
		storage.setPower(new DoubleValue(pwr));
	}

	private class VoltageListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			WriteContainer container = new WriteContainer();

			if (!initialized) {
				storage.setStateOfCharge(new DoubleValue((value.doubleValue() - storage.getVoltageMin())
						/ (storage.getVoltageMax() - storage.getVoltageMin()) * 100));
				initialized = true;
			}

			try {
				if (voltage != value.doubleValue() || time - storage.getStateOfCharge().getTime() >= 60000) {

					switch (mode) {
					case CHARGE_FROM_GRID:
						container.addDouble(setpointCurrentImport,
								-setpoint.getLatestValue().doubleValue() / value.doubleValue(), time);
						break;
					case FEED_INTO_GRID:
						if (value.doubleValue() > storage.getVoltageMin() && !dischargeProtection) {
							container.addDouble(setpointCurrentExport,
									setpoint.getLatestValue().doubleValue() / value.doubleValue(), time);
						} else if (value.doubleValue() <= storage.getVoltageMin() || dischargeProtection) {
							setpoint.setLatestValue(new DoubleValue(0, time));
							container.addDouble(setpointCurrentExport,
									setpoint.getLatestValue().doubleValue() / voltage, time);
						}
						break;
					case DEFAULT:
					case DISABLED:
						break;
					}

					voltage = value.doubleValue();
					storage.socEstimation(time);
					getDischargeProtection(time);
					getOverchargeProtection();

					if (powers.containsKey("pwr_fuelcell") && overchargeProtection
							&& powers.get("pwr_fuelcell").getLatestValue().doubleValue() > 0) {
						set(new DoubleValue(powers.get("pwr_fuelcell").getLatestValue().doubleValue(), time));
					}

					doWrite(container);
					setStoragePower();
				}
			} catch (Exception e) {
				logger.warn("Invalid value exception in voltage listener: {}", e);
			}

		}
	}

	private class CurrentListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			double storageCurrent = value.doubleValue();

			try {
				if (value.doubleValue() != current || time - storage.getStateOfCharge().getTime() >= 60000) {
					current = value.doubleValue();

					for (String key : keys) {
						switch (key) {
						case "pwr_photovoltaik":
							storageCurrent -= powers.get(key).getLatestValue().doubleValue() / voltage;
							break;
						case "pwr_fuelcell":
							storageCurrent -= powers.get(key).getLatestValue().doubleValue() / voltage;
							break;
						}
					}

					if (!Double.isNaN(storageCurrent)) {
						storage.setCurrent(new DoubleValue(storageCurrent));
					} else {
						storage.setCurrent(new DoubleValue(0));
					}

					if (initialized) {
						storage.socEstimation(value.getTime());
						getDischargeProtection(value.getTime());
						getOverchargeProtection();
					}

					if (powers.containsKey("pwr_fuelcell") && overchargeProtection
							&& powers.get("pwr_fuelcell").getLatestValue().doubleValue() >= 0) {
						set(powers.get("pwr_fuelcell").getLatestValue());
					}

					setStoragePower();
				}
			} catch (Exception e) {
				logger.warn("Error in current listener: {}", e);
			}

		}
	}
}