package de.thebox.control.feature.circulation.pump;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.feature.circulation.Circulation;
import de.thebox.control.feature.circulation.CirculationTemperature;


public class CirculationPump extends Circulation {

	private final String stateKey;
	private final ValueListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private final int intervalMin;

	private final Double referenceTemperatureMax;
	private final Double referenceTemperatureMin;
	private final double deltaTemperatureMin;

	public CirculationPump(ControlService control, Preferences prefs) throws ComponentConfigException {
		super(control, prefs);
		
		CirculationPumpConfig config = new CirculationPumpConfig(prefs);
		this.deltaTemperatureMin = config.getDeltaTemperatureMin();
		this.referenceTemperatureMax = config.getReferenceTemperatureMax();
		this.referenceTemperatureMin = config.getReferenceTemperatureMin();
		registerTemperatureListener(config.getReferenceTemperature(), CirculationTemperature.REF);
		
		this.stateKey = config.getState();
		this.stateListener = registerStateListener(stateKey);

		this.intervalMin = config.getIntervalMin();
	}

	private ValueListener registerStateListener(String id) {
		ValueListener stateListener = new ValueListener() {
			
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
		control.registerValueListener(id, stateListener);
		
		return stateListener;
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		if (stateListener != null) {
			control.deregisterValueListener(stateKey, stateListener);
		}
	}

	public void start() {
		control.writeValue(stateKey, new BooleanValue(true));
	}

	public void stop() {
		control.writeValue(stateKey, new BooleanValue(false));
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
