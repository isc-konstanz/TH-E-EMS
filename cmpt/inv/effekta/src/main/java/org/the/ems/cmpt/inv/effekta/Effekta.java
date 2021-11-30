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
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(scope = ServiceScope.BUNDLE, service = InverterService.class, configurationPid = InverterService.PID
		+ ".effekta", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Effekta extends Inverter<EffektaBattery> {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);
	protected Mode mode = Mode.DEFAULT;
	private Value setpointPower = DoubleValue.emptyValue();
	private double voltage = 0;
	private double current = 0;
	private boolean initialized = false;
	private boolean dischargeProtection = false;
	private String[] keys;
	private long timeModeChanged = 0;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

	@Configuration
	protected long modeUpdatePause;

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

		container.addDouble(storage.getCurrentMax(), Math.min((int) getMaxPower() / storage.getVoltageMin(), 200),
				System.currentTimeMillis());
		setpointPower = new DoubleValue(0, System.currentTimeMillis());
		setpoint.setLatestValue(setpointPower);
		setMode(container, Mode.DEFAULT);

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

		if (time - timeModeChanged >= modeUpdatePause) {
			setpointPower = value;

			try {
				if (setpointPower.doubleValue() == 0 && mode != Mode.DISABLED) {
					setMode(container, Mode.DISABLED);
				} else if (setpointPower.doubleValue() < 0 && mode != Mode.CHARGE_FROM_GRID) {
					setMode(container, Mode.CHARGE_FROM_GRID);
				} else if (setpointPower.doubleValue() > 0 && mode != Mode.FEED_INTO_GRID
						&& storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()
						|| !dischargeProtection) {
					setMode(container, Mode.FEED_INTO_GRID);
				}
			} catch (InvalidValueException e) {
				logger.warn("Error setting mode to feed_into_the_grid. Obligatory value missing: {}", e);
			}
			onSetpointUpdate(container);
		}
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpointPower.getTime();

		switch (mode) {
		case DEFAULT:
			break;
		case CHARGE_FROM_GRID:
			container.addDouble(setpointCurrentImport, -setpointPower.doubleValue() / voltage, time);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					if (!dischargeProtection) {
						container.addDouble(setpointCurrentExport, setpointPower.doubleValue() / voltage, time);
					} else {
						container.addDouble(setpointCurrentExport,
								Math.max(setpointPower.doubleValue() / voltage, this.getMaxPower() / (voltage * 2)),
								time);
					}
				}
			} catch (Exception e) {
				logger.warn("Obligatory value missing on setpoint update: {}", e.getMessage());
			}
			break;
		case DISABLED:
			break;
		}
	}

	private void setMode(WriteContainer container, Mode mode) {
		long time = setpointPower.getTime();
		timeModeChanged = time;

		switch (mode) {
		case DEFAULT:
			container.addDouble(operationMode, 0x8000L, time + messagesDelay);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 5);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 6);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 7);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 8);
			this.mode = Mode.DEFAULT;
			break;

		case CHARGE_FROM_GRID:
			container.addDouble(storage.getVoltageSetpoint(), storage.getVoltageMax(), time + messagesDelay);
			container.addDouble(operationMode, 0x8000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x4000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0xdfffL, time + messagesDelay * 4);
			container.addDouble(operationMode, 0xefffL, time + messagesDelay * 5);
			container.addDouble(operationMode, 0xf7ffL, time + messagesDelay * 6);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 7);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 8);
			this.mode = Mode.CHARGE_FROM_GRID;
			break;

		case FEED_INTO_GRID:
			container.addDouble(storage.getVoltageSetpoint(), storage.getVoltageMin(), time + messagesDelay);
			container.addDouble(operationMode, 0x7fffL, time + messagesDelay * 2);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 5);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0400L, time + messagesDelay * 7);
			container.addDouble(operationMode, 0x0200L, time + messagesDelay * 8);
			this.mode = Mode.FEED_INTO_GRID;
			break;

		case DISABLED:
			if (this.mode == Mode.FEED_INTO_GRID) {
				container.addDouble(operationMode, 0xdfffL, time + messagesDelay * 2);
				container.addDouble(operationMode, 0xefffL, time + messagesDelay * 3);
				container.addDouble(operationMode, 0xf7ffL, time + messagesDelay * 4);
				container.addDouble(operationMode, 0x8000L, time + messagesDelay * 5);
			}
			if (this.mode == Mode.CHARGE_FROM_GRID) {
				container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 2);
				container.addDouble(operationMode, 0x2000L, time + messagesDelay * 3);
			}
			if (this.mode == Mode.DEFAULT) {
				container.addDouble(operationMode, 0xefffL, time + messagesDelay * 2);
				container.addDouble(operationMode, 0xf7ffL, time + messagesDelay * 3);
			}
			this.mode = Mode.DISABLED;
			break;
		}
	}

	public void getDischargeProtection(Long time)
			throws InvalidValueException, UnsupportedOperationException, EnergyManagementException {
		if (storage.getStateOfCharge().doubleValue() <= storage.getMinStateOfCharge() && mode == Mode.FEED_INTO_GRID) {
			dischargeProtection = true;
			set(new DoubleValue(0, time));
			logger.warn("Low SOC! Feeding into the Grid stopped.");
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

	private class SetpointUpdater implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			this.onSetpointChange();
		}

		protected void onSetpointChange() {
			try {
				WriteContainer container = new WriteContainer();
				doSetpointUpdate(container);
				doWrite(container);

			} catch (EnergyManagementException e) {
				logger.warn("Obligatory value missing: {}", e.getMessage());
			}
		}

		protected void doSetpointUpdate(WriteContainer container) throws ComponentException {
			onSetpointUpdate(container);
		};
	}

	private class VoltageListener extends SetpointUpdater {

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
					if (mode == Mode.CHARGE_FROM_GRID) {
						container.addDouble(setpointCurrentImport, -setpointPower.doubleValue() / value.doubleValue(),
								time);
						if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge() + 5) {
							dischargeProtection = false;
						}

					} else if (mode == Mode.FEED_INTO_GRID) {
						if (value.doubleValue() > storage.getVoltageMin() && !dischargeProtection) {
							container.addDouble(setpointCurrentExport,
									setpointPower.doubleValue() / value.doubleValue(), time);
							dischargeProtection = false;
						} else if (value.doubleValue() <= storage.getVoltageMin() || dischargeProtection) {
							setpointPower = new DoubleValue(0, time);
							container.addDouble(setpointCurrentExport, setpointPower.doubleValue() / voltage, time);
							dischargeProtection = true;
						}
					}

					voltage = value.doubleValue();

					if (powers.containsKey("pwr_fuelcell")
							&& storage.getStateOfCharge().doubleValue() >= storage.getMaxStateOfCharge()
							&& powers.get("pwr_fuelcell").getLatestValue().doubleValue() > 0) {
						set(new DoubleValue(powers.get("pwr_fuelcell").getLatestValue().doubleValue()));
					}

					storage.socEstimation(value.getTime());
					if (!dischargeProtection) {
						getDischargeProtection(value.getTime());
					}
					doWrite(container);
					setStoragePower();
				}
			} catch (Exception e) {
				logger.warn("Invalid value exception in voltage listener: {}", e);
			}

		}
	}

	private class CurrentListener extends SetpointUpdater {

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
						if (!dischargeProtection) {
							getDischargeProtection(value.getTime());
						}
					}

					if (powers.containsKey("pwr_fuelcell")
							&& storage.getStateOfCharge().doubleValue() >= storage.getMaxStateOfCharge()
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