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
	private double soc = 0;
	private double voltage = 0;
	private double current = 0;
	private double dischargeProtectionPower;
	private boolean initialized = false;
	private boolean dischargeProtection = false;
	private String[] keys;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

	@Configuration
	private Channel operationMode;

	@Configuration
	private Channel socEstimation;

	@Configuration(mandatory = false)
	private Channel pwrInverter;

	@Configuration(mandatory = false)
	private Channel pwrCHP;

	@Configuration(value = "pwr_*")
	protected ChannelCollection powers;

	@Configuration
	private Channel currentInv;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateOfChargeListener(new StateOfChargeListener());
		storage.registerCurrentListener(new CurrentListener());
		storage.registerVoltageListener(new VoltageListener());

		initialize();
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		setpointPower = value;

		if (setpointPower.doubleValue() == 0 && mode != Mode.DISABLED) {
			setMode(container, Mode.DISABLED);
		} else if (setpointPower.doubleValue() < 0 && mode != Mode.CHARGE_FROM_GRID) {
			setMode(container, Mode.CHARGE_FROM_GRID);
		} else if (setpointPower.doubleValue() > 0 && mode != Mode.FEED_INTO_GRID
				&& soc >= storage.getMinStateOfCharge()) {
			setMode(container, Mode.FEED_INTO_GRID);
		}

		onSetpointUpdate(container);
	}

	public void initialize() {
		WriteContainer container = new WriteContainer();
		container.addDouble(storage.getMaxCurrent(), Math.min((int) getMaxPower() / storage.getMinVoltage(), 200),
				System.currentTimeMillis());
		setpointPower = DoubleValue.emptyValue();
		setMode(container, Mode.DEFAULT);
		dischargeProtectionPower = this.getMaxPower();

		if (pwrCHP != null) {
			pwrCHP.setLatestValue(new DoubleValue(0));
		}

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
		storage.setPower(new DoubleValue(0));
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpointPower.getTime();

		switch (mode) {
		case DEFAULT:
			break;
		case CHARGE_FROM_GRID:
			container.addDouble(storage.getCurrentImport(), -setpointPower.doubleValue() / voltage, time);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					if (!dischargeProtection) {
						container.addDouble(storage.getCurrentExport(), setpointPower.doubleValue() / voltage, time);
					} else {
						container.addDouble(storage.getCurrentExport(), Math.max(setpointPower.doubleValue() / voltage,
								dischargeProtectionPower / (voltage * 2)), time);
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
			container.addDouble(storage.getVoltageSetpoint(), storage.getMaxVoltage(), time + messagesDelay);
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
			container.addDouble(storage.getVoltageSetpoint(), storage.getMinVoltage(), time + messagesDelay);
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

	private void setSOCEstimation(Double vol, Double cur, Long time) throws InvalidValueException {
		long socTime = socEstimation.getLatestValue().getTime();
		double energy;
		double socEstimationNew;

		if (vol.doubleValue() >= storage.getMaxVoltage()) {
			socEstimation.setLatestValue(new DoubleValue(100));
		} else if (vol.doubleValue() <= storage.getMinVoltage()) {
			socEstimation.setLatestValue(new DoubleValue(0));
		} else {
			energy = cur * vol * (time - socTime) / (1000 * 3600);
			socEstimationNew = socEstimation.getLatestValue().doubleValue()
					+ energy / (storage.getCapacity() * 1000) * 100;
			socEstimationNew = Math.max(0, socEstimationNew);
			socEstimationNew = Math.min(100, socEstimationNew);
			socEstimation.setLatestValue(new DoubleValue(socEstimationNew));
		}
	}

	private void setPower() throws InvalidValueException {
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

	private class StateOfChargeListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			soc = value.doubleValue();
			WriteContainer container = new WriteContainer();

			try {
				if (soc < storage.getMinStateOfCharge() && mode == Mode.FEED_INTO_GRID) {
					container.addDouble(storage.getCurrentImport(), 0, time);
					setMode(container, Mode.CHARGE_FROM_GRID);
					doWrite(container);
				}
			} catch (Exception e) {
				logger.warn("Error in SOC listener: {}", e.getMessage());
			}
		}
	}

	private class VoltageListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			WriteContainer container = new WriteContainer();

			if (!initialized) {
				socEstimation.setLatestValue(new DoubleValue((value.doubleValue() - storage.getMinVoltage())
						/ (storage.getMaxVoltage() - storage.getMinVoltage()) * 100));
				try {
					container.addDouble(socEstimation, socEstimation.getLatestValue().doubleValue(),
							time + messagesDelay);
					doWrite(container);
					initialized = true;
				} catch (Exception e) {
					logger.warn("SOC initialization failed: {}", e.getMessage());
				}
			}

			if (voltage != value.doubleValue()) {
				try {
					setSOCEstimation(value.doubleValue(), current, value.getTime());
				} catch (InvalidValueException e1) {
					logger.warn("Could not set soc estimation value: {}", e1.getMessage());
				}

				if (mode == Mode.CHARGE_FROM_GRID) {
					container.addDouble(storage.getCurrentImport(), setpointPower.doubleValue() / value.doubleValue(),
							time);
					if (value.doubleValue() >= storage.getMinVoltage() + 1) {
						dischargeProtection = false;
					}
				} else if (mode == Mode.FEED_INTO_GRID) {
					if (value.doubleValue() > storage.getMinVoltage() + 0.1) {
						container.addDouble(storage.getCurrentExport(),
								-setpointPower.doubleValue() / value.doubleValue(), time);
						dischargeProtection = false;
					} else if (value.doubleValue() <= storage.getMinVoltage() || dischargeProtection) {
						setpointPower = new DoubleValue(setpointPower.doubleValue() - 1000);
						container.addDouble(storage.getCurrentExport(), -setpointPower.doubleValue() / voltage, time);
						dischargeProtection = true;
					}
				}

				try {
					doWrite(container);
				} catch (EnergyManagementException e) {
					logger.warn("Write channel error. Could not set new values: {}", e.getMessage());
				}
				
				voltage = value.doubleValue();
				
				if (pwrCHP != null) {
					try {
						if (mode != Mode.CHARGE_FROM_GRID) {
							storage.setPower(
									new DoubleValue(current * voltage + pwrCHP.getLatestValue().doubleValue()));
						} else if (mode != Mode.FEED_INTO_GRID) {
							storage.setPower(
									new DoubleValue(-current * voltage + pwrCHP.getLatestValue().doubleValue()));
						}
					} catch (InvalidValueException e) {
						e.printStackTrace();
					}
				} else {
					storage.setPower(new DoubleValue(current * voltage));
				}
				
				try {
					setPower();
				} catch (InvalidValueException e) {
					logger.warn("Error calculating soc estimation: {}", e);
				}
			}
		}
	}

	private class CurrentListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			long socTime = 0;

			if (initialized) {
				try {
					socTime = socEstimation.getLatestValue().getTime();
				} catch (InvalidValueException e) {
					logger.warn("SOC estimation error. No SOC value yet: {}", e.getMessage());
				}
			}

			if (value.doubleValue() != current || time - socTime >= 60000) {
				current = value.doubleValue();

				if (initialized) {
					try {
						setSOCEstimation(voltage, value.doubleValue(), value.getTime());
					} catch (InvalidValueException e) {
						logger.warn("Error calculating soc estimation: {}", e);
					}
				}
				
				try {
					setPower();
				} catch (InvalidValueException e) {
					logger.warn("Error calculating soc estimation: {}", e);
				}

				powers.get("pwr_inverter").setLatestValue(new DoubleValue(current * voltage));

				Double pwr = current * voltage;
				try {
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
				} catch (InvalidValueException e) {
					logger.warn("Error caculating storage power: {}", e);
				}
			}

//			if (pwrInverter != null) {
//				pwrInverter.setLatestValue(new DoubleValue(current * voltage));
//			}

//			int i = 0;
//			double[] values = new double[2];
//			for (Channel power : powers.values()) {
//				try {
//					values[i] = power.getLatestValue().doubleValue();
//				} catch (InvalidValueException e) {
//				}
//						i++;
//			}
//			for (Channel power : powers.values()) {
//				switch (power.getId()) {
//				case "inv_power":
//					power.setLatestValue(new DoubleValue(current * voltage));
//					break;
//				case "fc_power":
//					try {
//						power.setLatestValue(new DoubleValue(current * voltage - storage.getPower().doubleValue()));
//					} catch (InvalidValueException e) {
//					}
//					break;
//				}
//			}
//			
//			if (pwrCHP != null) {
//				try {
//					storage.setPower(new DoubleValue(current * voltage - pwrCHP.getLatestValue().doubleValue()));
//				} catch (InvalidValueException e) {
//					logger.warn("Error setting storage power: {}", e);
//				}
//			} else {
//				storage.setPower(new DoubleValue(current * voltage));
//			}

		}
	}
}