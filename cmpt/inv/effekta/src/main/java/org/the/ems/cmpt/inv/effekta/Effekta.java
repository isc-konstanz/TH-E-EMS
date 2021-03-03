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
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		setpointPower = value;

		if (setpointPower.doubleValue() == 0 && mode != Mode.DEFAULT) {
			setMode(container, Mode.DEFAULT);
		} else if (setpointPower.doubleValue() > 0 && mode != Mode.CHARGE_FROM_GRID) {
			setMode(container, Mode.CHARGE_FROM_GRID);
		} else if (setpointPower.doubleValue() < 0 && mode != Mode.FEED_INTO_GRID
				&& soc >= storage.getMinStateOfCharge()) {
			setMode(container, Mode.FEED_INTO_GRID);
		}

		onSetpointUpdate(container);
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpointPower.getTime();

		switch (mode) {
		case DEFAULT:
		case CHARGE_FROM_GRID:
			container.addDouble(storage.getChargeCurrent(), setpointPower.doubleValue() / voltage, time);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					container.addDouble(storage.getDischargeCurrent(), -setpointPower.doubleValue() / voltage, time);
					container.addDouble(storage.getVoltageSetpoint(), storage.getDischargeVoltage(),
							time + messagesDelay);
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
			break;

		case CHARGE_FROM_GRID:
			container.addDouble(storage.getVoltageSetpoint(), storage.getChargeVoltage(), time + messagesDelay);
			container.addDouble(operationMode, 0x8000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x4000L, time + messagesDelay * 3);
//			  container.addDouble(operationMode, 0x2000L, time + messagesDelay * 4);
//			  container.addDouble(operationMode, 0x1000L, time + messagesDelay * 5);
//            container.addDouble(operationMode, 0x0800L, time + messagesDelay * 6);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 4);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 5);
//            container.addDouble(operationMode, 0x0100L, time + messagesDelay * 9);

			this.mode = Mode.CHARGE_FROM_GRID;
			break;

		case FEED_INTO_GRID:
			container.addDouble(operationMode, 0x7fffL, time + messagesDelay * 2);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 3);
//			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 4);
//			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 5);
//            container.addDouble(operationMode, 0x0800L, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0400L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0x0200L, time + messagesDelay * 5);
//            container.addDouble(operationMode, 0x0100L, time + messagesDelay * 9);

			this.mode = Mode.FEED_INTO_GRID;
			break;

		case DISABLED:
			container.addDouble(operationMode, 0x7fffL, time + messagesDelay);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0xefffL, time + messagesDelay * 4);
			container.addDouble(operationMode, 0xf7ffL, time + messagesDelay * 5);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 6);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 7);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 8);
			break;
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
					container.addDouble(storage.getChargeCurrent(), 10, time);
					container.addDouble(storage.getVoltageSetpoint(), voltage, time + 100);
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
			try {
				if (voltage != value.doubleValue()) {
					if (mode == Mode.CHARGE_FROM_GRID) {
						container.addDouble(storage.getChargeCurrent(), setpointPower.doubleValue() / voltage, time);
					}
					if (mode == Mode.FEED_INTO_GRID) {
						container.addDouble(storage.getDischargeCurrent(), -setpointPower.doubleValue() / voltage,
								time);
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
//			
//			storage.setSocEstimation();
			if (value.doubleValue() != current) {
				current = value.doubleValue();
				storage.setPower(new DoubleValue(current * voltage));
			}

		}
	}
}