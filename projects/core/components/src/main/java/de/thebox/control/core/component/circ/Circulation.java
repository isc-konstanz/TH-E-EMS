package de.thebox.control.core.component.circ;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.circ.CirculationTemperatureListener.CirculationTemperatureCallbacks;
import de.thebox.control.core.component.circ.pump.CirculationPump;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;

public class Circulation implements CirculationTemperatureCallbacks {

	private Channel deltaTemperature;
	private Value outTemperatureLast = DoubleValue.emptyValue();
	private Value inTemperatureLast = DoubleValue.emptyValue();
	private final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();

    /**
     * Interface used by {@link Circulation} to notify the {@link CirculationPump} about events
     */
    public interface CirculationCallbacks {

    	public void onTemperatureReferenceUpdated(Value delta);

    	public void onTemperatureDeltaUpdated(Value delta);
    }

    /**
     * The Circulations current callback object, which is used to notify of events
     */
	private volatile CirculationCallbacks callbacks = null;

	public Circulation(ControlService control, Preferences prefs) throws ConfigurationException {
		try {
			if (prefs.nodeExists(CirculationConfig.SECTION)) {
				CirculationConfig config = new CirculationConfig(prefs);
				
				this.deltaTemperature = control.getChannel(config.getDeltaTemperature());
				registerTemperatureListener(control.getChannel(config.getInTemperature()), CirculationTemperature.IN);
				registerTemperatureListener(control.getChannel(config.getOutTemperature()), CirculationTemperature.OUT);
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid circulation configuration: " + e.getMessage());
		}
	}

	public void registerTemperatureListener(Channel channel, CirculationTemperature type) {
		CirculationTemperatureListener listener = new CirculationTemperatureListener(this, type, channel);
		temperatureListeners.add(listener);
	}

	public void deactivate() {
		for (CirculationTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}
	}

	public void register(CirculationCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public void deregister() {
		this.callbacks = null;
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
			if (callbacks != null) {
				callbacks.onTemperatureReferenceUpdated(temperature);
			}
			break;
		}
		if (type == CirculationTemperature.OUT || type == CirculationTemperature.IN) {
			if (outTemperatureLast.getTime() == inTemperatureLast.getTime()) {
				
				double delta = outTemperatureLast.doubleValue() - inTemperatureLast.doubleValue();
				Value value = new DoubleValue(delta, outTemperatureLast.getTime());
				
				deltaTemperature.setLatestValue(value);
				if (callbacks != null) {
					callbacks.onTemperatureDeltaUpdated(value);
				}
			}
		}
	}
}
