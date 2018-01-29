package de.thebox.control.component.hp.gondzik.ventilation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.hp.gondzik.ventilation.CabinetTemperatureListener.CabinetTemperatureCallbacks;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;

public class CabinetVentilation implements CabinetTemperatureCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(CabinetVentilation.class);

	private final Channel state;
	private final ChannelListener stateListener;

	private Value stateValueLast = null;
	private volatile long startTimeLast = 0;
	private final int intervalMin;

	private final double temperatureMax;
	private final double temperatureTolerance;
	private final List<CabinetTemperatureListener> temperatureListeners = new ArrayList<CabinetTemperatureListener>();
	private final List<CabinetTemperature> temperatureHighFlags = new ArrayList<CabinetTemperature>(CabinetTemperature.values().length);

	public CabinetVentilation(ControlService control, Preferences prefs) throws ComponentConfigException {
		logger.info("Activating TH-E Control cabinet ventilation management");
		CabinetVentilationConfig config = new CabinetVentilationConfig(prefs);
		this.intervalMin = config.getIntervalMin();
		this.temperatureMax = config.getTemperatureMax();
		this.temperatureTolerance = config.getTemperatureTolerance();
		try {
			registerTemperatureListener(control.getChannel(config.getTopTemperatureKey()), CabinetTemperature.TOP);
			registerTemperatureListener(control.getChannel(config.getCenterTemperatureKey()), CabinetTemperature.CENTER);
			registerTemperatureListener(control.getChannel(config.getBottomTemperatureKey()), CabinetTemperature.BOTTOM);
			
			this.state = control.getChannel(config.getStateKey());
			this.stateListener = registerStateListener(state);
		} catch (UnknownChannelException e) {
			throw new ComponentConfigException("Invalid ventilation configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener stateListener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					if (stateValueLast != null) {
						boolean state = value.booleanValue();
						if (state && !stateValueLast.booleanValue()) {
							startTimeLast = value.getTimestamp();
						}
						else if (!state && temperatureHighFlags.size() > 0) {
							temperatureHighFlags.clear();
						}
					}
					stateValueLast = value;
				}
			}
		};
		return stateListener;
	}

	private void registerTemperatureListener(Channel channel, CabinetTemperature type) {
		CabinetTemperatureListener listener = new CabinetTemperatureListener(this, type, channel);
		temperatureListeners.add(listener);
	}

	public void deactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		for (CabinetTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}
	}

	public void start() {
		state.writeValue(new BooleanValue(true));
	}

	public void stop() {
		state.writeValue(new BooleanValue(false));
	}

	@Override
	public synchronized void onTemperatureReceived(CabinetTemperature type, Double temperature) {
		if (temperature > temperatureMax + temperatureTolerance && !temperatureHighFlags.contains(type)) {
			temperatureHighFlags.add(type);
			start();
		}
		else if (temperature < temperatureMax - temperatureTolerance &&
				System.currentTimeMillis() - startTimeLast >= intervalMin) {
			
			if (stateValueLast != null && stateValueLast.booleanValue()) {
				temperatureHighFlags.remove(type);
				if (temperatureHighFlags.size() == 0) {
					stop();
				}
			}
		}
	}

}
