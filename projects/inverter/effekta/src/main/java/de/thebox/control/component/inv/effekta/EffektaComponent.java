package de.thebox.control.component.inv.effekta;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.transform.SourceLocator;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentWriteContainer;
import de.thebox.control.core.component.inv.InverterComponent;
import de.thebox.control.core.component.inv.consumption.PowerListener;
import de.thebox.control.core.component.inv.consumption.PowerListener.PowerCallbacks;
import de.thebox.control.core.component.inv.consumption.PowerType;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.FloatValue;
import de.thebox.control.core.data.IntValue;
import de.thebox.control.core.data.ShortValue;
import de.thebox.control.core.data.Value;

@Component
public class EffektaComponent extends InverterComponent implements PowerCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(EffektaComponent.class);

	private final static String ID = "Effekta";
	
	private Battery battery;
	
	protected Channel consumptionPower;
	protected Channel dcPower1;
	protected Channel dcPower2;
	
	protected Value acPowerLast = IntValue.emptyValue();
	protected Value dc1PowerLast = IntValue.emptyValue();
	protected Value dc2PowerLast = IntValue.emptyValue();
	
	protected final List<PowerListener> powerListeners = new ArrayList<PowerListener>();

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences prefs) throws ControlException {
		super.activate(prefs);
		
		EffektaConfig config = new EffektaConfig(prefs);
		
		consumptionPower = control.getChannel(config.getConsumptionPower());
		dcPower1 = control.getChannel(config.getDCPower1());
		dcPower2 = control.getChannel(config.getDCPower2());
		
		registerPowerValueListener(consumptionPower, PowerType.AC);
		registerPowerValueListener(dcPower1, PowerType.DC1);
		registerPowerValueListener(dcPower2, PowerType.DC2);
	}
	
	private void registerPowerValueListener(Channel channel, PowerType type) {
		PowerListener listener = new PowerListener(this, type, channel);
		powerListeners.add(listener);
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		for (PowerListener listener: powerListeners) {
			listener.deregister();
		}
	}
	
	private int getBatteryPower (int dcPower, int consumption, int objective) {
		int power = Math.abs(consumption + objective - dcPower);
		return power;
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		switch(type) {
		case AC:
			acPowerLast = power;
			break;
		case DC1:
			dc1PowerLast = power;
			break;
		case DC2:
			dc2PowerLast = power;
			break;
		default:
			break;
		}
		if (acPowerLast.getTime() == dc1PowerLast.getTime() &&
				acPowerLast.getTime() == dc2PowerLast.getTime()) {
			
//			double consumption = acPowerLast.doubleValue();
//			if (consumption > 0) {
//				consumption += dc1PowerLast.doubleValue();
//			}
//			consumption -= dc2PowerLast.doubleValue();
//			
//			if (consumption < 0) {
//				consumption = 0;
//			}
//			Value value = new DoubleValue(consumption, acPowerLast.getTime());
//			
//			this.consumption.setLatestValue(value);
//			for (ValueListener listener : listeners) {
//				listener.onValueReceived(value);
//			}
		}
		
		onUpdate();
	}

	@Override
	public void objective(ComponentWriteContainer container, Value value) throws ComponentException {
		int objective = value.intValue();
		int dcPower = dc1PowerLast.intValue() + dc2PowerLast.intValue();
		int consumption = acPowerLast.intValue();
		Value batteryPower = new IntValue(getBatteryPower(dcPower, consumption, objective));
		
		// Inverter firmware does not except to set the charge/discharge current below 10A
		if (battery.getMinimumPower() > batteryPower.intValue()) { 
			battery.setMode(container, Mode.DISABLED);
		}
		else if (objective < 0) { // receive energy from grid
			int objectiveReceive = Math.abs(objective);
			
			if (objectiveReceive > consumption - dcPower) {
				battery.setChargeCurrent(container, batteryPower);
				battery.setMode(container, Mode.CHARGE_FROM_GRID);
			}
			else {
				battery.setDischargeCurrent(container, batteryPower);
				battery.setMode(container, Mode.DEFAULT);			
			}	
		}
		else if (objective > 0) { //feed energy to grid
			if (objective > dcPower - consumption){
				battery.setDischargeCurrent(container, batteryPower);
				battery.setMode(container, Mode.DISCHARGE_TO_GRID);
			}
			else {
				battery.setChargeCurrent(container, batteryPower);
				battery.setMode(container, Mode.DEFAULT);	
			}
		}
		else { // default
			if (dcPower > consumption){
				battery.setChargeCurrent(container, batteryPower);
			}
			else {
				battery.setDischargeCurrent(container, batteryPower);
			}
			
			battery.setMode(container, Mode.DEFAULT);
		}
	}
}