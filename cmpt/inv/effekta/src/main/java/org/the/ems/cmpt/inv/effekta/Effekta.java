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
	private long delay = 0;
	private double soc = 0;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	private Channel dischargeCurrent;

	@Configuration
	private Channel chargeCurrent;

	@Configuration
	private Channel batteryKeepVoltage;

	@Configuration
	private Channel operationMode;

	@Configuration
	protected long messagesDelay;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateOfChargeListener(new StateOfChargeListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		setpointPower = value;

		try {
			if (setpointPower.doubleValue() >= 0 && mode != Mode.CHARGE_FROM_GRID) {
				setMode(container, Mode.CHARGE_FROM_GRID);
			} else if (setpointPower.doubleValue() < 0 && mode != Mode.FEED_INTO_GRID
					&& soc >= storage.getMinStateOfCharge()) {
				setMode(container, Mode.FEED_INTO_GRID);
			}
		} catch (Exception e) {
			logger.warn("Obligatory storage value missing: {}", e.getMessage());
		}
		onSetpointUpdate(container);
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpointPower.getTime();

		switch (mode) {
		case DEFAULT:
		case CHARGE_FROM_GRID:
			container.addDouble(chargeCurrent, setpointPower.doubleValue() / storage.getChargeVoltage(), time + delay);
			container.addDouble(batteryKeepVoltage, storage.getChargeVoltage(), time + delay + 100);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					container.addDouble(dischargeCurrent, -setpointPower.doubleValue() / storage.getChargeVoltage(),
							time + delay);
					container.addDouble(batteryKeepVoltage, storage.getDischargeVoltage(), time + delay + 100);
				}
			} catch (Exception e) {
				logger.warn("Obligatory storage value missing: {}", e.getMessage());
			}
			break;
		case DISABLED:
			// TODO: what to do if disabled?
			break;
		}
	}

	private void setMode(WriteContainer container, Mode mode) {
		long time = setpointPower.getTime();
		delay = 0;

		switch (mode) {
		case DEFAULT:
		case CHARGE_FROM_GRID:
			container.addDouble(operationMode, 0x8000L, time + messagesDelay * 0);
			container.addDouble(operationMode, 0x4000L, time + messagesDelay * 1);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 5);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 7);

			this.mode = Mode.CHARGE_FROM_GRID;
			delay = messagesDelay * 8;
			break;

		case FEED_INTO_GRID:
			container.addDouble(operationMode, 0x7fffL, time + messagesDelay * 0);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 1);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0x0400L, time + messagesDelay * 5);
			container.addDouble(operationMode, 0x0200L, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 7);

			this.mode = Mode.FEED_INTO_GRID;
			delay = messagesDelay * 8;
			break;

		case DISABLED:
			break;
		}
	}

//	private class PowerValueListener implements ValueListener{
//
//		@Override
//		public void onValueReceived(Value value) {
//			switch(type) {
//			case AC:
//				acPowerLast = power;
//				break;
//			case DC1:
//				dc1PowerLast = power;
//				break;
//			case DC2:
//				dc2PowerLast = power;
//				break;
//			default:
//				break;
//			}
//			if (acPowerLast.getTime() == dc1PowerLast.getTime() &&
//					acPowerLast.getTime() == dc2PowerLast.getTime()) {
//				
////				double consumption = acPowerLast.doubleValue();
////				if (consumption > 0) {
////					consumption += dc1PowerLast.doubleValue();
////				}
////				consumption -= dc2PowerLast.doubleValue();
////				
////				if (consumption < 0) {
////					consumption = 0;
////				}
////				Value value = new DoubleValue(consumption, acPowerLast.getTime());
////				
////				this.consumption.setLatestValue(value);
////				for (ValueListener listener : listeners) {
////					listener.onValueReceived(value);
////				}
//			}
//			
//			onUpdate();
//		}
//		
//	}

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
					container.addDouble(chargeCurrent, 1, time + delay);
					container.addDouble(batteryKeepVoltage, storage.getVoltage().doubleValue(), time + delay + 100);
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

}