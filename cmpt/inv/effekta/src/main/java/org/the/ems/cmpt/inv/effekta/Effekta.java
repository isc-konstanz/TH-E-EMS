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
	private Value setpoint = DoubleValue.emptyValue();
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

//	@Configuration(value="power_*")
//	protected ChannelCollection power;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateOfChargeListener(new StateOfChargeListener());
//		inputPower.registerValueListener(new SetpointUpdater());
//		if (activeError) {
//			activePower.registerValueListener(new SetpointUpdater());
//		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
//		inputPower.deregister();
//		activePower.deregister();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		setpoint = value;
		onSetpointUpdate(container);
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		long time = setpoint.getTime();
		updateMode(container);

		switch (mode) {
		case DEFAULT:
		case CHARGE_FROM_GRID:
			//TODO: abfragen ob alle channels (setpoint,storage) schon einen wert haben!
			container.addDouble(chargeCurrent, setpoint.doubleValue(), time + delay);
			container.addDouble(batteryKeepVoltage, storage.getChargeVoltage(), time + delay + 100);
			break;
		case FEED_INTO_GRID:
			container.addDouble(dischargeCurrent, setpoint.doubleValue(), time + delay);
			container.addDouble(batteryKeepVoltage, storage.getDischargeVoltage(), time + delay + 100);
			break;
		case DISABLED:
			// TODO: what to do if disabled?
			break;
		}
	}

	private void updateMode(WriteContainer container) {
		long time = setpoint.getTime();
		delay = 0;

		if (setpoint.doubleValue() >= 0 && mode != Mode.CHARGE_FROM_GRID) {
			container.addDouble(operationMode, 0x8000L, time + messagesDelay * 0);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 1);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0xfbffL, time + messagesDelay * 5);
			container.addDouble(operationMode, 0xfdffL, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 7);

			setMode(Mode.CHARGE_FROM_GRID);
			delay = messagesDelay * 8;
		}

		if (setpoint.doubleValue() < 0 && mode != Mode.FEED_INTO_GRID) {
			container.addDouble(operationMode, 0x7fffL, time + messagesDelay * 0);
			container.addDouble(operationMode, 0xbfffL, time + messagesDelay * 1);
			container.addDouble(operationMode, 0x2000L, time + messagesDelay * 2);
			container.addDouble(operationMode, 0x1000L, time + messagesDelay * 3);
			container.addDouble(operationMode, 0x0800L, time + messagesDelay * 4);
			container.addDouble(operationMode, 0x0400L, time + messagesDelay * 5);
			container.addDouble(operationMode, 0x0200L, time + messagesDelay * 6);
			container.addDouble(operationMode, 0x0100L, time + messagesDelay * 7);

			setMode(Mode.FEED_INTO_GRID);
			delay = messagesDelay * 8;
		}
	}

	private void setMode(Mode operationMode) {
		mode = operationMode;
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
				logger.warn("Error updating value: {}", e.getMessage());
			}
		}

		protected void doSetpointUpdate(WriteContainer container) throws ComponentException {
			onSetpointUpdate(container);
		};

	}

	private class StateOfChargeListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			soc = value.doubleValue();
			WriteContainer container = new WriteContainer();
			
//			if (soc < storage.getMinStateOfCharge() && !trickleCurrentFlag) {
//				trickleCurrentFlag = true;
//				container.addDouble(setpointPower, -storage.getTricklePower(), System.currentTimeMillis());
//				try {
//					doWrite(container);
//				} catch (EnergyManagementException e) {
//					logger.warn("Could not set trickle current: {}", e.getMessage());
//				}
//			}
//			else if (soc >= storage.getMinStateOfCharge() + socHyst && trickleCurrentFlag) {
//				trickleCurrentFlag = false;
//				container.addDouble(setpointPower, -setpointControl.doubleValue(), System.currentTimeMillis());
//				try {
//					doWrite(container);
//				} catch (EnergyManagementException e) {
//					logger.warn("Could not reset trickle current: {}", e.getMessage());
//				}
//				
//			}
		}
	}

}