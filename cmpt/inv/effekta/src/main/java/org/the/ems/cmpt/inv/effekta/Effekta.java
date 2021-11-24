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
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE, 
	service = InverterService.class,
	configurationPid = InverterService.PID+".effekta", 
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Effekta extends Inverter<EffektaBattery> {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	protected Mode mode = Mode.DEFAULT;

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration
	protected long messagesDelay;

	@Configuration
	private Channel operationMode;

//	@Configuration
//	private Channel batteryKeepVoltage;

	private ValueListener socListener;
	private double soc = 0;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		socListener = new StateOfChargeListener();
		storage.registerStateOfChargeListener(socListener);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregisterStateOfChargeListener(socListener);
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		double setpointPower = value.doubleValue();
		long timestamp = value.getTime();
		
		if (setpointPower == 0 && mode != Mode.DEFAULT) {
			setMode(container, timestamp, Mode.DEFAULT);
		}
		else if (setpointPower > 0 && mode != Mode.CHARGE_FROM_GRID) {
			setMode(container, timestamp, Mode.CHARGE_FROM_GRID);
		}
		else if (setpointPower < 0 && mode != Mode.FEED_INTO_GRID && soc >= storage.getMinStateOfCharge()) {
			setMode(container, timestamp, Mode.FEED_INTO_GRID);
		}
		switch (mode) {
		case DEFAULT:
		case CHARGE_FROM_GRID:
			double chargeCurrent = setpointPower / storage.getChargeVoltage();
			container.addDouble(storage.chargeCurrent, chargeCurrent, timestamp);
//			container.addDouble(batteryKeepVoltage, storage.getChargeVoltage(), time + delay + 100);
			break;
		case FEED_INTO_GRID:
			try {
				if (storage.getStateOfCharge().doubleValue() >= storage.getMinStateOfCharge()) {
					double dischargeCurrent = -setpointPower / storage.getChargeVoltage();
					container.addDouble(storage.dischargeCurrent, dischargeCurrent, timestamp);
//					container.addDouble(batteryKeepVoltage, storage.getDischargeVoltage(), time + delay + 100);
				}
			} catch (InvalidValueException e) {
				logger.warn("Invalid value updating setpoint: {}", e.getMessage());
			}
			break;
		case DISABLED:
			break;
		}
	}

	@Override
	protected void onInterrupt() throws ComponentException {
		// TODO: Verify if selected mode is actually set
//		try {
//			byte operationModeByte = operationMode.getLatestValue().byteValue();
//			if (operationModeByte != mode.getByte()) {
//		        WriteContainer container = new WriteContainer();
//				setMode(container, System.currentTimeMillis(), mode);
//		        doWrite(container);
//			}
//		} catch (InvalidValueException e) {
//			logger.warn();
//		}
	}

	private void setMode(WriteContainer container, long timestamp, Mode mode) {
		// TODO: Check if bit needs to be set before setting it to massively improve speed
//		BitSet operationModeBits;
//		try {
//			long operationModeBytes = operationMode.getLatestValue().longValue();
//			operationModeBits = BitSet.valueOf(new long[]{operationModeBytes});
//			
//		} catch (InvalidValueException e) {
//			operationModeBits = new BitSet(8);
//		}
		switch (mode) {
		case DEFAULT:
//			long timestampDelayed = timestamp;
//			if (!operationModeBits.get(0)) {
//				container.addLong(operationMode, 0x8000b, timestampDelayed);
//				timestampDelayed += messagesDelay;
//			}
			container.addLong(operationMode, 0x8000L, timestamp + messagesDelay);
			container.addLong(operationMode, 0xbfffL, timestamp + messagesDelay * 2);
			container.addLong(operationMode, 0x2000L, timestamp + messagesDelay * 3);
			container.addLong(operationMode, 0x1000L, timestamp + messagesDelay * 4);
			container.addLong(operationMode, 0x0800L, timestamp + messagesDelay * 5);
			container.addLong(operationMode, 0xfbffL, timestamp + messagesDelay * 6);
			container.addLong(operationMode, 0xfdffL, timestamp + messagesDelay * 7);
			container.addLong(operationMode, 0x0100L, timestamp + messagesDelay * 8);
			break;

		case CHARGE_FROM_GRID:
			container.addLong(operationMode, 0x8000L, timestamp + messagesDelay);
			container.addLong(operationMode, 0x4000L, timestamp + messagesDelay * 2);
			container.addLong(operationMode, 0x2000L, timestamp + messagesDelay * 3);
			container.addLong(operationMode, 0x1000L, timestamp + messagesDelay * 4);
            container.addLong(operationMode, 0x0800L, timestamp + messagesDelay * 5);
            container.addLong(operationMode, 0xfbffL, timestamp + messagesDelay * 6);
            container.addLong(operationMode, 0xfdffL, timestamp + messagesDelay * 7);
            container.addLong(operationMode, 0x0100L, timestamp + messagesDelay * 8);
			break;

		case FEED_INTO_GRID:
			container.addLong(operationMode, 0x7fffL, timestamp + messagesDelay);
			container.addLong(operationMode, 0xbfffL, timestamp + messagesDelay * 2);
			container.addLong(operationMode, 0x2000L, timestamp + messagesDelay * 3);
			container.addLong(operationMode, 0x1000L, timestamp + messagesDelay * 4);
            container.addLong(operationMode, 0x0800L, timestamp + messagesDelay * 5);
            container.addLong(operationMode, 0x0400L, timestamp + messagesDelay * 6);
            container.addLong(operationMode, 0x0200L, timestamp + messagesDelay * 7);
            container.addLong(operationMode, 0x0100L, timestamp + messagesDelay * 8);
			break;

		case DISABLED:
			container.addLong(operationMode, 0x7fffL, timestamp + messagesDelay);
			container.addLong(operationMode, 0xbfffL, timestamp + messagesDelay * 2);
			container.addLong(operationMode, 0x2000L, timestamp + messagesDelay * 3);
			container.addLong(operationMode, 0xefffL, timestamp + messagesDelay * 4);
			container.addLong(operationMode, 0xf7ffL, timestamp + messagesDelay * 5);
			container.addLong(operationMode, 0xfbffL, timestamp + messagesDelay * 6);
			container.addLong(operationMode, 0xfdffL, timestamp + messagesDelay * 7);
			container.addLong(operationMode, 0x0100L, timestamp + messagesDelay * 8);
			break;
		}
		this.mode = mode;
	}

	private class StateOfChargeListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			long time = value.getTime();
			soc = value.doubleValue();
			WriteContainer container = new WriteContainer();

			try {
				if (soc < storage.getMinStateOfCharge() && mode == Mode.FEED_INTO_GRID) {
					container.addDouble(storage.chargeCurrent, 10, time);
//					container.addDouble(batteryKeepVoltage, storage.getVoltage().doubleValue(), time + delay + 100);
					setMode(container, time, Mode.CHARGE_FROM_GRID);
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