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
	private boolean dischargeProtection = false;
	private double dischargeProtectionPower;
	private boolean initialized = false;

	@Configuration
	private Channel socEstimation;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

	@Configuration
	private Channel operationMode;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateOfChargeListener(new StateOfChargeListener());
		storage.registerVoltageListener(new VoltageListener());
		storage.registerCurrentListener(new CurrentListener());

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
		} else if (setpointPower.doubleValue() > 0 && mode != Mode.CHARGE_FROM_GRID) {
			setMode(container, Mode.CHARGE_FROM_GRID);
		} else if (setpointPower.doubleValue() < 0 && mode != Mode.FEED_INTO_GRID
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

		try {
			doWrite(container);
		} catch (EnergyManagementException e) {
			logger.warn("Effekta activation error: {}", e.getMessage());
		}
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpointPower.getTime();

		switch (mode) {
		case DEFAULT:
			break;
		case CHARGE_FROM_GRID:
			container.addDouble(storage.getChargeCurrent(), setpointPower.doubleValue() / voltage, time);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					if (!dischargeProtection) {
						container.addDouble(storage.getDischargeCurrent(), -setpointPower.doubleValue() / voltage,
								time);
					} else {
						container.addDouble(storage.getDischargeCurrent(),
								Math.min(-setpointPower.doubleValue() / voltage, dischargeProtectionPower / voltage),
								time);
					}
				}
			} catch (Exception e) {
				logger.warn("Obligatory storage value missing: {}", e.getMessage());
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
					container.addDouble(storage.getChargeCurrent(), 0, time);
					setMode(container, Mode.CHARGE_FROM_GRID);
					try {
						doWrite(container);
					} catch (EnergyManagementException e) {
						logger.warn("Could not set new power value: {}", e.getMessage());
					}
				}
			} catch (Exception e) {
				logger.warn("Obligatory storage value missing: {}", e.getMessage());
			}
		}
	}

	private class VoltageListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			WriteContainer container = new WriteContainer();

			if (!initialized) {
				container.addDouble(socEstimation, (value.doubleValue() - storage.getMinVoltage())
						/ (storage.getMaxVoltage() - storage.getMinVoltage()) * 100, time + messagesDelay);
				initialized = true;
				logger.info("SOC estimation initialialized with {} %", value.doubleValue());
			}

			try {
				if (voltage != value.doubleValue()) {
					setSOCEstimation(value.doubleValue(), current, value.getTime());

					if (mode == Mode.CHARGE_FROM_GRID) {
						container.addDouble(storage.getChargeCurrent(),
								setpointPower.doubleValue() / value.doubleValue(), time);
					} else if (mode == Mode.FEED_INTO_GRID) {
						if (value.doubleValue() > storage.getMinVoltage() + 0.1) {
							container.addDouble(storage.getDischargeCurrent(),
									-setpointPower.doubleValue() / value.doubleValue(), time);
							dischargeProtection = false;
						} else if (value.doubleValue() <= storage.getMinVoltage() || dischargeProtection) {
							setpointPower = new DoubleValue(setpointPower.doubleValue() - 200);
							container.addDouble(storage.getDischargeCurrent(), -setpointPower.doubleValue() / voltage,
									time);
							dischargeProtection = true;
						}
					}

					try {
						doWrite(container);
					} catch (EnergyManagementException e) {
						logger.warn("Could not set new value: {}", e.getMessage());
					}
					voltage = value.doubleValue();
					storage.setPower(new DoubleValue(voltage * current));
				}
			} catch (Exception e) {
				logger.warn("Obligatory storage value missing: {}", e.getMessage());
			}

		}
	}

	private class CurrentListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			long socTime = 0;
			try {
				socTime = socEstimation.getLatestValue().getTime();

			} catch (InvalidValueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (value.doubleValue() != current || time - socTime >= 60000) {
				if (initialized) {
					try {
						setSOCEstimation(voltage, value.doubleValue(), value.getTime());
					} catch (InvalidValueException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

//					if (dischargeProtection) {
//						WriteContainer container = new WriteContainer();
//						//TODO:
//						try {
//							doWrite(container);
//						} catch (EnergyManagementException e) {
//							logger.warn("Could not set new value: {}", e.getMessage());
//						}
//					}
				}

				current = value.doubleValue();
				storage.setPower(new DoubleValue(current * voltage));
			}
		}
	}
}