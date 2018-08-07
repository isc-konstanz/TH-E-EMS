package de.thebox.control.core.component.circ;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.Value;

public class CirculationTemperatureListener extends ChannelListener {

	/**
	 * Interface used to notify the {@link Circulation} 
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

	public CirculationTemperatureListener(CirculationTemperatureCallbacks callbacks, CirculationTemperature type, Channel channel) {
		super(channel);
		
		this.callbacks = callbacks;
		this.type = type;
	}

	public CirculationTemperature getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		callbacks.onTemperatureReceived(type, value);
	}
}
