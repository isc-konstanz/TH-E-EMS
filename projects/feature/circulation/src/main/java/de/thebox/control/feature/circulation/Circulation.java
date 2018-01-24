package de.thebox.control.feature.circulation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.feature.circulation.CirculationTemperatureListener.CirculationTemperatureCallbacks;


public class Circulation implements CirculationTemperatureCallbacks {

	protected final ControlService control;

	protected final String deltaTemperature;
	protected Value outTemperatureLast = null;
	protected Value inTemperatureLast = null;
	protected final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();

	public Circulation(ControlService control, Preferences configs) throws ComponentConfigException {
		this.control = control;
		
		this.deltaTemperature = configs.get(CirculationConst.DELTA_TEMPERATURE_KEY, null);
		registerTemperatureListener(configs.get(CirculationConst.IN_TEMPERATURE_KEY, null), CirculationTemperature.IN);
		registerTemperatureListener(configs.get(CirculationConst.OUT_TEMPERATURE_KEY, null), CirculationTemperature.OUT);
	}

	protected void registerTemperatureListener(String id, CirculationTemperature type) throws ComponentConfigException {
		if (id != null) {
			CirculationTemperatureListener listener = new CirculationTemperatureListener(this, type, id);
			
			temperatureListeners.add(listener);
			control.registerValueListener(id, listener);
		}
		else throw new ComponentConfigException("Unable to find configured cabinet temperature value");
	}

	public void deactivate() {
		for (CirculationTemperatureListener listener: temperatureListeners) {
			control.deregisterValueListener(listener.getId(), listener);
		}
	}

	@Override
	public synchronized void onTemperatureReceived(CirculationTemperature type, Value temperature) {
		switch(type) {
		case IN:
			inTemperatureLast = temperature;
			break;
		case OUT:
			outTemperatureLast = temperature;
			break;
		case REF:
			onTemperatureReferenceUpdated(temperature);
			break;
		}
		if (type == CirculationTemperature.OUT || type == CirculationTemperature.IN) {
			if (outTemperatureLast != null && inTemperatureLast != null &&
					outTemperatureLast.getTimestamp().equals(inTemperatureLast.getTimestamp())) {
				
				double delta = outTemperatureLast.doubleValue() - inTemperatureLast.doubleValue();
				onTemperatureDeltaUpdated(new DoubleValue(delta, outTemperatureLast.getTimestamp()));
			}
		}
	}

	protected  void onTemperatureReferenceUpdated(Value delta) {
		// Placeholder implementation for the circulation pump control
	}

	protected  void onTemperatureDeltaUpdated(Value delta) {
		control.setLatestValue(deltaTemperature, delta);
	}
}
