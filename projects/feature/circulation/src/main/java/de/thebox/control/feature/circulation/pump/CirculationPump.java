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

	private final String stateValue;
	private final ValueListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private final int intervalMin;

	private final Double referenceTemperatureMax;
	private final Double referenceTemperatureMin;
	private final double deltaTemperatureMin;

	public CirculationPump(ControlService control, Preferences configs) throws ComponentConfigException {
		super(control, configs);
		
		this.deltaTemperatureMin = configs.getDouble(CirculationPumpConst.MIN_DELTA_TEMPERATURE_KEY, CirculationPumpConst.MIN_DELTA_TEMPERATURE_DEFAULT);
		
		this.referenceTemperatureMax = configs.getDouble(CirculationPumpConst.MAX_REF_TEMPERATURE_KEY, Double.NaN);
		this.referenceTemperatureMin = configs.getDouble(CirculationPumpConst.MIN_REF_TEMPERATURE_KEY, Double.NaN);
		registerTemperatureListener(configs.get(CirculationPumpConst.REF_TEMPERATURE_KEY, null), CirculationTemperature.REF);
		
		this.stateValue = configs.get(CirculationPumpConst.PUMP_STATE_KEY, null);
		this.intervalMin = configs.getInt(CirculationPumpConst.INTERVAL_KEY, CirculationPumpConst.INTERVAL_DEFAULT)*60000;
		this.stateListener = registerStateListener(stateValue);
	}

	private ValueListener registerStateListener(String id) throws ComponentConfigException {
		if (id != null) {
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
		throw new ComponentConfigException("Unable to find configured state value");
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		if (stateListener != null) {
			control.deregisterValueListener(stateValue, stateListener);
		}
	}

	public void start() {
		control.writeValue(stateValue, new BooleanValue(true));
	}

	public void stop() {
		control.writeValue(stateValue, new BooleanValue(false));
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
