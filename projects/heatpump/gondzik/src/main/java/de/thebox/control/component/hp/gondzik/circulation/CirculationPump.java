package de.thebox.control.component.hp.gondzik.circulation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.component.hp.gondzik.circulation.CirculationTemperatureListener.CirculationTemperatureCallbacks;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;


public class CirculationPump implements CirculationTemperatureCallbacks {

	private final ControlService control;

	private final String deltaTemperature;
	private Value outTemperatureLast = null;
	private Value inTemperatureLast = null;
	private final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();

	public CirculationPump(ControlService control, Preferences configs) throws ComponentConfigException {
		this.control = control;
		
		this.deltaTemperature = configs.get(CirculationPumpConst.DELTA_TEMPERATURE_KEY, null);
		registerTemperatureListener(configs.get(CirculationPumpConst.OUT_TEMPERATURE_KEY, null), CirculationTemperature.OUT);
		registerTemperatureListener(configs.get(CirculationPumpConst.IN_TEMPERATURE_KEY, null), CirculationTemperature.IN);
	}

	private void registerTemperatureListener(String id, CirculationTemperature type) throws ComponentConfigException {
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
		case OUT:
			outTemperatureLast = temperature;
			break;
		case IN:
			inTemperatureLast = temperature;
			break;
		}
		if (type == CirculationTemperature.OUT || type == CirculationTemperature.IN) {
			if (outTemperatureLast != null && inTemperatureLast != null &&
					outTemperatureLast.getTimestamp().equals(inTemperatureLast.getTimestamp())) {
				
				double delta = outTemperatureLast.doubleValue() - inTemperatureLast.doubleValue();
				control.setLatestValue(deltaTemperature, new DoubleValue(delta));
			}
		}
	}
}
