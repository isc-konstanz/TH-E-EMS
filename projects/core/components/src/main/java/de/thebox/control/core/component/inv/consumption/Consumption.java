package de.thebox.control.core.component.inv.consumption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentCallbacks;
import de.thebox.control.core.component.inv.InverterConfig;
import de.thebox.control.core.component.inv.consumption.PowerListener.PowerCallbacks;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public class Consumption implements PowerCallbacks {

	protected final ComponentCallbacks callbacks;

	private Channel consumption;
	protected Map<PowerType, Value> powerValues;
	protected List<PowerListener> powerListeners;
	protected final ConsumptionListener listener;

	protected class ConsumptionListener implements ValueListener {

		@Override
		public void onValueReceived(Value objective) {
			callbacks.onUpdate();
		}
	}

	public Consumption(ComponentCallbacks callbacks, ControlService control, InverterConfig inverter, Preferences prefs)
			throws ConfigurationException {
		this.callbacks = callbacks;
		this.listener = new ConsumptionListener();
		try {
			this.consumption = control.getChannel(inverter.getConsumptionPower());
			this.consumption.registerValueListener(listener);
			
			if (prefs.nodeExists(ConsumptionConfig.SECTION)) {
				ConsumptionConfig config = new ConsumptionConfig(prefs);
				
				powerValues = new HashMap<PowerType, Value>();
				powerListeners = new ArrayList<PowerListener>();
				registerPowerValueListener(control.getChannel(config.getBatteryPower()), PowerType.BATTERY);
				registerPowerValueListener(control.getChannel(config.getAcPower()), PowerType.AC);
				if (!config.hasDcPower2()) {
					registerPowerValueListener(control.getChannel(config.getDcPower()), PowerType.DC1);
				}
				else {
					registerPowerValueListener(control.getChannel(config.getDcPower1()), PowerType.DC1);
					registerPowerValueListener(control.getChannel(config.getDcPower2()), PowerType.DC2);
				}
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid consumption configuration: " + e.getMessage());
		}
	}

	private void registerPowerValueListener(Channel channel, PowerType type) {
		powerValues.put(type, DoubleValue.emptyValue());
		
		PowerListener listener = new PowerListener(this, type, channel);
		powerListeners.add(listener);
	}

	public void deactivate() {
		for (PowerListener listener: powerListeners) {
			listener.deregister();
		}
		consumption.deregisterValueListener(listener);
	}

	public Value getLatestValue() {
		return consumption.getLatestValue();
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		long time = power.getTime();
		
		powerValues.put(type, power);
		for (Value value : powerValues.values()) {
			if (value.getTime() != time) {
				return;
			}
		}
		double consumption = powerValues.get(PowerType.AC).doubleValue();
		if (consumption > 0) {
			consumption += powerValues.get(PowerType.DC1).doubleValue();
			if (powerValues.containsKey(PowerType.DC2)) {
				consumption += powerValues.get(PowerType.DC2).doubleValue();
			}
		}
		consumption -= powerValues.get(PowerType.BATTERY).doubleValue();
		
		if (consumption < 0) {
			consumption = 0;
		}
		this.consumption.setLatestValue(new DoubleValue(consumption, time));
	}

}
