package org.the.ems.cmpt.inv.effekta;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID+".effekta",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Effekta extends Inverter {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	@Configuration
	protected Channel dischargeCurrent;
	
	@Configuration
	protected Channel chargeCurrent;
	
	@Configuration
	protected Channel batteryKeepVoltage;
	
	@Configuration
	protected Channel operationMode;
	
	
//	@Configuration(value="power_*")
//	protected ChannelCollection power;
//
// 
//	protected Value acPowerLast = IntValue.emptyValue();
//	protected Value dc1PowerLast = IntValue.emptyValue();
//	protected Value dc2PowerLast = IntValue.emptyValue();
//	
//	
//	protected final List<PowerListener> powerListeners = new ArrayList<PowerListener>();


	@Override
	public void onActivate(Configurations configs) throws ComponentException  {
		super.onActivate(configs);
		
	}
	
	
	@Override
	public void onDeactivate() {
		super.onDeactivate();

	}
	
	
	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
		
		try {
			if (value.doubleValue() >= 0) {
				// draw energy from grid (normal mode)
				container.addDouble(operationMode, 0x8000L, time + 0);
				container.addDouble(operationMode, 0xbfffL, time + 100);
				container.addDouble(operationMode, 0x2000L, time + 200);
				container.addDouble(operationMode, 0x1000L, time + 300);
				
				container.addDouble(operationMode, 0x0800L, time + 400);
				container.addDouble(operationMode, 0xfbffL, time + 500);
				container.addDouble(operationMode, 0xfdffL, time + 600);
				container.addDouble(operationMode, 0x0100L, time + 700);
				
				container.addDouble(chargeCurrent, value.doubleValue(), time + 800);
				container.addDouble(batteryKeepVoltage, battery.getVoltage().doubleValue()+2, time + 90);
//				container.addDouble(batteryKeepVoltage, 52, time + 900);
				
			}
			else {
				// feed energy into grid (feed-into-the-grid-mode)			
				container.addDouble(operationMode, 0x7fffL, time + 0);
				container.addDouble(operationMode, 0xbfffL, time + 100);
				container.addDouble(operationMode, 0x2000L, time + 200);
				container.addDouble(operationMode, 0x1000L, time + 300);
				
				container.addDouble(operationMode, 0x0800L, time + 400);
				container.addDouble(operationMode, 0x0400L, time + 500);
				container.addDouble(operationMode, 0x0200L, time + 600);
				container.addDouble(operationMode, 0x0100L, time + 700);
				
				container.addDouble(dischargeCurrent, value.doubleValue(), time + 800);
			}
			
		} catch (Exception e) {
			logger.debug("Unable to updating inverter setpoint: {}", e.getMessage());
		}
	}
	
//	
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
	



//	private int getBatteryPower (int dcPower, int consumption, int objective) {
//		int power = Math.abs(consumption + objective - dcPower);
//		return power;
//	}
//
//	@Override
//	public void onPowerValueReceived(PowerType type, Value power) {
//		switch(type) {
//		case AC:
//			acPowerLast = power;
//			break;
//		case DC1:
//			dc1PowerLast = power;
//			break;
//		case DC2:
//			dc2PowerLast = power;
//			break;
//		default:
//			break;
//		}
//		if (acPowerLast.getTime() == dc1PowerLast.getTime() &&
//				acPowerLast.getTime() == dc2PowerLast.getTime()) {
//			
////			double consumption = acPowerLast.doubleValue();
////			if (consumption > 0) {
////				consumption += dc1PowerLast.doubleValue();
////			}
////			consumption -= dc2PowerLast.doubleValue();
////			
////			if (consumption < 0) {
////				consumption = 0;
////			}
////			Value value = new DoubleValue(consumption, acPowerLast.getTime());
////			
////			this.consumption.setLatestValue(value);
////			for (ValueListener listener : listeners) {
////				listener.onValueReceived(value);
////			}
//		}
//		
//		onUpdate();
//	}
//
//	@Override
//	public void objective(ComponentWriteContainer container, Value value) throws ComponentException {
//		int objective = value.intValue();
//		int dcPower = dc1PowerLast.intValue() + dc2PowerLast.intValue();
//		int consumption = acPowerLast.intValue();
//		Value batteryPower = new IntValue(getBatteryPower(dcPower, consumption, objective));
//		
//		// Inverter firmware does not except to set the charge/discharge current below 10A
//		if (battery.getMinimumPower() > batteryPower.intValue()) { 
//			battery.setMode(container, Mode.DISABLED);
//		}
//		else if (objective < 0) { // receive energy from grid
//			int objectiveReceive = Math.abs(objective);
//			
//			if (objectiveReceive > consumption - dcPower) {
//				battery.setChargeCurrent(container, batteryPower);
//				battery.setMode(container, Mode.CHARGE_FROM_GRID);
//			}
//			else {
//				battery.setDischargeCurrent(container, batteryPower);
//				battery.setMode(container, Mode.DEFAULT);			
//			}	
//		}
//		else if (objective > 0) { //feed energy to grid
//			if (objective > dcPower - consumption){
//				battery.setDischargeCurrent(container, batteryPower);
//				battery.setMode(container, Mode.DISCHARGE_TO_GRID);
//			}
//			else {
//				battery.setChargeCurrent(container, batteryPower);
//				battery.setMode(container, Mode.DEFAULT);	
//			}
//		}
//		else { // default
//			if (dcPower > consumption){
//				battery.setChargeCurrent(container, batteryPower);
//			}
//			else {
//				battery.setDischargeCurrent(container, batteryPower);
//			}
//			
//			battery.setMode(container, Mode.DEFAULT);
//		}
//	}

	@Reference(
		cardinality = ReferenceCardinality.MANDATORY,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindElectricalEnergyStorageService(ElectricalEnergyStorageService service) {
		battery = service;
	}

	protected void unbindElectricalEnergyStorageService(ElectricalEnergyStorageService service) {
		battery = null;
	}

}