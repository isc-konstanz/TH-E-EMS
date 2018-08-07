package de.thebox.control.core.component.inv.consumption;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.inv.InverterConfig;
import de.thebox.control.core.component.inv.consumption.PowerListener.PowerCallbacks;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.ValueListener;

public class Consumption implements PowerCallbacks {

	private Channel consumption;
	protected Value batPowerLast = DoubleValue.emptyValue();
	protected Value acPowerLast = DoubleValue.emptyValue();
	protected Value dcPowerLast = DoubleValue.emptyValue();
	protected final List<PowerListener> powerListeners = new ArrayList<PowerListener>();

	protected final List<ValueListener> listeners = new ArrayList<ValueListener>();

	public Consumption(ControlService control, Preferences prefs) throws ConfigurationException {
		InverterConfig inv = new InverterConfig(prefs);
		try {
			this.consumption = control.getChannel(inv.getConsumptionPower());
			
			if (prefs.nodeExists(ConsumptionConfig.SECTION)) {
				ConsumptionConfig config = new ConsumptionConfig(prefs);
				
				registerPowerValueListener(control.getChannel(config.getBatteryPower()), PowerType.BATTERY);
				registerPowerValueListener(control.getChannel(config.getAcPower()), PowerType.AC);
				registerPowerValueListener(control.getChannel(config.getDcPower()), PowerType.DC);
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid consumption configuration: " + e.getMessage());
		}
	}

	private void registerPowerValueListener(Channel channel, PowerType type) {
		PowerListener listener = new PowerListener(this, type, channel);
		powerListeners.add(listener);
	}

	public void deactivate() {
		for (PowerListener listener: powerListeners) {
			listener.deregister();
		}
	}

	public void register(ValueListener listener) {
		listeners.add(listener);
	}

	public void deregister(ValueListener listener) {
		listeners.remove(listener);
	}

	public Value getLatestValue() {
		return consumption.getLatestValue();
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		switch(type) {
		case BATTERY:
			batPowerLast = power;
			break;
		case AC:
			acPowerLast = power;
			break;
		case DC:
			dcPowerLast = power;
			break;
		}
		if (acPowerLast.getTime() == dcPowerLast.getTime() &&
				acPowerLast.getTime() == batPowerLast.getTime()) {
			
			double consumption = acPowerLast.doubleValue();
			if (consumption > 0) {
				consumption += dcPowerLast.doubleValue();
			}
			consumption -= batPowerLast.doubleValue();
			
			if (consumption < 0) {
				consumption = 0;
			}
			Value value = new DoubleValue(consumption, acPowerLast.getTime());
			
			this.consumption.setLatestValue(value);
			for (ValueListener listener : listeners) {
				listener.onValueReceived(value);
			}
		}
	}

}
