package de.thebox.control.component.inv.energydepot.consumption;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.component.inv.energydepot.consumption.PowerListener.PowerCallbacks;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public class Consumption implements PowerCallbacks {

	private Channel consumption;
	protected Value batPowerLast = DoubleValue.emptyValue();
	protected Value acPowerLast = DoubleValue.emptyValue();
	protected Value dcPowerLast = DoubleValue.emptyValue();
	protected final List<PowerListener> powerListeners = new ArrayList<PowerListener>();
	protected final List<ValueListener> consListeners = new ArrayList<ValueListener>();

	public Consumption(ControlService control, Preferences prefs) throws ComponentException {
		ConsumptionConfig config = new ConsumptionConfig(prefs);
		try {
			this.consumption = control.getChannel(config.getConsumptionPower());
			registerPowerValueListener(control.getChannel(config.getBatteryPower()), PowerType.BATTERY);
			registerPowerValueListener(control.getChannel(config.getAcPower()), PowerType.AC);
			registerPowerValueListener(control.getChannel(config.getDcPower()), PowerType.DC);
			
		} catch (UnknownChannelException e) {
			throw new ComponentConfigException("Invalid consumption configuration: " + e.getMessage());
		}
	}

	private void registerPowerValueListener(Channel channel, PowerType type) {
		PowerListener listener = new PowerListener(this, type, channel);
		powerListeners.add(listener);
	}

	public void register(ValueListener listener) {
		consListeners.add(listener);
	}

	public void deregister(ValueListener listener) {
		consListeners.remove(listener);
	}

	public void deactivate() {
		for (PowerListener listener: powerListeners) {
			listener.deregister();
		}
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
		if (acPowerLast.getTimestamp() == dcPowerLast.getTimestamp() &&
				acPowerLast.getTimestamp() == batPowerLast.getTimestamp()) {
			
			double consumption = acPowerLast.doubleValue();
			if (consumption > 0) {
				consumption += dcPowerLast.doubleValue();
			}
			consumption -= batPowerLast.doubleValue();
			
			if (consumption < 0) {
				consumption = 0;
			}
			this.consumption.setLatestValue(new DoubleValue(consumption, acPowerLast.getTimestamp()));
		}
	}

}
