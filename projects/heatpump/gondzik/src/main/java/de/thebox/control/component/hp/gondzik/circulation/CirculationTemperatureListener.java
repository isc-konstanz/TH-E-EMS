package de.thebox.control.component.hp.gondzik.circulation;

import java.util.LinkedList;
import java.util.List;

import de.thebox.control.core.ControlValueListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public class CirculationTemperatureListener implements ControlValueListener {
	
	private final static int MOVING_AVG_SIZE = 6;
	
	/**
	 * Interface used to notify the {@link CirculationPump} 
	 * implementation about changed temperatures
	 */
	public interface CirculationTemperatureCallbacks {
		
		public void onTemperatureReceived(CirculationTemperature type, Value temperature);
	}
	
	/**
	 * The Listeners' current callback object, which is notified of changed temperatures
	 */
	private final CirculationTemperatureCallbacks callbacks;

	private final CirculationTemperature type;
	private final String id;
	
	private List<Double> temperatures = new LinkedList<Double>();
	private double temperatureSum = -1;
	
	public CirculationTemperatureListener(CirculationTemperatureCallbacks callbacks, CirculationTemperature type, String id) {
		this.callbacks = callbacks;
		this.type = type;
		this.id = id;
	}
	
	public CirculationTemperature getType() {
		return type;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public void onValueReceived(Value value) {
		if (value != null) {
			Double temperature = value.doubleValue();
			
			if (temperatureSum == -1) {
				temperatureSum = temperature;
				temperatures.add(temperature);
			}
			else {
				while (temperatures.size() >= MOVING_AVG_SIZE) {
					temperatureSum -= temperatures.remove(0);
				}
				temperatures.add(temperature);
				temperatureSum += temperature;
			}
			
			Double average = temperatureSum/temperatures.size();
			callbacks.onTemperatureReceived(type, new DoubleValue(average, value.getTimestamp()));
		}
	}
}
