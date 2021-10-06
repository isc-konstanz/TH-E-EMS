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

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

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
		registerCurrentListener(new CurrentListener());

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
		} else
			try {
				if (setpointPower.doubleValue() > 0 && mode != Mode.FEED_INTO_GRID
						&& storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					setMode(container, Mode.FEED_INTO_GRID);
				}
			} catch (InvalidValueException e) {
				logger.warn("Error setting mode to feed_into_the_grid. Obligatory value missing: {}", e);
			}

		onSetpointUpdate(container);
	}

	public void initialize() {
		WriteContainer container = new WriteContainer();
		container.addDouble(storage.getCurrentMax(), Math.min((int) getMaxPower() / storage.getVoltageMin(), 200),
				System.currentTimeMillis());
		setpointPower = DoubleValue.emptyValue();
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
		storage.setPower(new DoubleValue(0));
		storage.setStateOfCharge(new DoubleValue(0));
	}

	void registerCurrentListener(ValueListener listener) {
		currentInv.registerValueListener(listener);
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

	private void setSOCEstimation(Double vol, Double cur, Long time) throws InvalidValueException {
		long socTime = storage.getStateOfCharge().getTime();
		double energy;
		double socEstimationNew;

		if (vol.doubleValue() >= storage.getVoltageMax()) {
			storage.setStateOfCharge(new DoubleValue(100));
		} else if (vol.doubleValue() <= storage.getVoltageMin()) {
			storage.setStateOfCharge(new DoubleValue(0));
		} else {
			energy = cur * vol * (time - socTime) / (1000 * 3600);
			socEstimationNew = storage.getStateOfCharge().doubleValue() - energy / (storage.getCapacity() * 1000) * 100;
			socEstimationNew = Math.max(0, socEstimationNew);
			socEstimationNew = Math.min(100, socEstimationNew);
			storage.setStateOfCharge(new DoubleValue(socEstimationNew));
		}

		try {
			if (storage.getStateOfCharge().doubleValue() < storage.getMinStateOfCharge()
					&& mode == Mode.FEED_INTO_GRID) {
				this.set(new DoubleValue(0));
				WriteContainer container = new WriteContainer();
				container.addDouble(setpointCurrentExport, 0, time);
				container.addDouble(setpointCurrentImport, 0, time + messagesDelay);
				doWrite(container);
			}
		} catch (Exception e) {
			logger.warn("Error in SOC discharge protection: {}", e.getMessage());
		}
	}

	private void setPowers() throws InvalidValueException {
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

			if (voltage != value.doubleValue()) {
				try {
					setSOCEstimation(value.doubleValue(), current, value.getTime());
				} catch (InvalidValueException e1) {
					logger.warn("Could not set soc estimation value: {}", e1.getMessage());
				}

				if (mode == Mode.CHARGE_FROM_GRID) {
					container.addDouble(setpointCurrentImport, -setpointPower.doubleValue() / value.doubleValue(),
							time);
					if (value.doubleValue() >= storage.getVoltageMin() + 1) {
						dischargeProtection = false;
					}
				} else if (mode == Mode.FEED_INTO_GRID) {
					if (value.doubleValue() > storage.getVoltageMin() + 0.1) {
						container.addDouble(setpointCurrentExport,
								setpointPower.doubleValue() / value.doubleValue(), time);
						dischargeProtection = false;
					} else if (value.doubleValue() <= storage.getVoltageMin() || dischargeProtection) {
						setpointPower = new DoubleValue(setpointPower.doubleValue() - 1000);
						container.addDouble(setpointCurrentExport, setpointPower.doubleValue() / voltage, time);
						dischargeProtection = true;
					}
				}

				try {
					doWrite(container);
				} catch (EnergyManagementException e) {
					logger.warn("Write channel error. Could not set new values: {}", e.getMessage());
				}

				voltage = value.doubleValue();

				try {
					setPowers();
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
					socTime = storage.getStateOfCharge().getTime();
				} catch (InvalidValueException e) {
					logger.warn("SOC estimation error. No SOC value yet: {}", e.getMessage());
				}
			}

			if (value.doubleValue() != current || time - socTime >= 60000) {
				current = value.doubleValue();
				storage.setCurrent(value);

				if (initialized) {
					try {
						setSOCEstimation(voltage, value.doubleValue(), value.getTime());
					} catch (InvalidValueException e) {
						logger.warn("Error calculating soc estimation: {}", e);
					}
				}

				try {
					setPowers();
				} catch (InvalidValueException e) {
					logger.warn("Error calculating power from current value: {}", e);
				}
			}
		}
	}
}