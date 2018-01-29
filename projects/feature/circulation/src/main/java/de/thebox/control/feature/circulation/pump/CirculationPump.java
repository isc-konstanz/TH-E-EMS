package de.thebox.control.feature.circulation.pump;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.feature.circulation.Circulation;
import de.thebox.control.feature.circulation.CirculationTemperature;

public class CirculationPump extends Circulation {

	private final Channel state;
	private final ChannelListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private final int intervalMin;

	private final Double referenceTemperatureMax;
	private final Double referenceTemperatureMin;
	private final double deltaTemperatureMin;

	public CirculationPump(ControlService control, Preferences prefs) throws ComponentConfigException {
		super(control, prefs);
		
		CirculationPumpConfig config = new CirculationPumpConfig(prefs);
		this.intervalMin = config.getIntervalMin();
		this.deltaTemperatureMin = config.getDeltaTemperatureMin();
		this.referenceTemperatureMax = config.getReferenceTemperatureMax();
		this.referenceTemperatureMin = config.getReferenceTemperatureMin();
		try {
			registerTemperatureListener(control.getChannel(config.getReferenceTemperature()), CirculationTemperature.REF);
			
			this.state = control.getChannel(config.getState());
			this.stateListener = registerStateListener(state);
			
		} catch (UnknownChannelException e) {
			throw new ComponentConfigException("Invalid circulation pump configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener stateListener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					boolean state = value.booleanValue();
					if (state && !stateValueLast.booleanValue()) {
						startTimeLast = value.getTimestamp();
					}
					stateValueLast = value;
				}
			}
		};
		return stateListener;
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		if (stateListener != null) {
			stateListener.deregister();
		}
	}

	public void start() {
		state.writeValue(new BooleanValue(true));
	}

	public void stop() {
		state.writeValue(new BooleanValue(false));
	}

	protected  void onTemperatureReferenceUpdated(Value delta) {
		if (!stateValueLast.booleanValue() && delta.doubleValue() > referenceTemperatureMax) {
			start();
		}
		else if (delta.doubleValue() < referenceTemperatureMin) {
			stop();
		}
	}

	protected  void onTemperatureDeltaUpdated(Value delta) {
		super.onTemperatureDeltaUpdated(delta);
		
		if (stateValueLast.booleanValue() && delta.doubleValue() <= deltaTemperatureMin && 
				System.currentTimeMillis() - startTimeLast >= intervalMin) {
			
			stop();
		}
	}
}
