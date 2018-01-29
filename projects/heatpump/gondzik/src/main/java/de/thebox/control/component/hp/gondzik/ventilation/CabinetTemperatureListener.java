package de.thebox.control.component.hp.gondzik.ventilation;

import java.util.LinkedList;
import java.util.List;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.Value;

public class CabinetTemperatureListener extends ChannelListener {

	private final static int MOVING_AVG_SIZE = 6;

	/**
	 * Interface used to notify the {@link CabinetVentilation} 
	 * implementation about changed temperatures
	 */
	public interface CabinetTemperatureCallbacks {
	
		public void onTemperatureReceived(CabinetTemperature type, Double temperature);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed temperatures
	 */
	private final CabinetTemperatureCallbacks callbacks;

	private final CabinetTemperature type;

	private List<Double> temperatures = new LinkedList<Double>();
	private double temperatureSum = -1;

	public CabinetTemperatureListener(CabinetTemperatureCallbacks callbacks, CabinetTemperature type, Channel channel) {
		super(channel);
		
		this.callbacks = callbacks;
		this.type = type;
	}

	public CabinetTemperature getType() {
		return type;
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
			callbacks.onTemperatureReceived(type, average);
		}
	}
}
