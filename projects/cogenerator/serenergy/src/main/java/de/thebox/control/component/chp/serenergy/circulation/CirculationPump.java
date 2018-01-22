package de.thebox.control.component.chp.serenergy.circulation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.component.chp.serenergy.circulation.CirculationTemperatureListener.CirculationTemperatureCallbacks;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.ControlValueListener;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;


public class CirculationPump implements CirculationTemperatureCallbacks {
	
	private final ControlService control;
	
	private final String stateValue;
	private final ControlValueListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private final int intervalMin;

	private final String deltaTemperature;
	private Value outTemperatureLast = null;
	private Value inTemperatureLast = null;
	private final double stackTemperatureMax;
	private final double deltaTemperatureMin;
	private final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();
	
	public CirculationPump(ControlService control, Preferences configs) throws ComponentConfigException {
		this.control = control;
		
		this.stateValue = configs.get(CirculationPumpConst.PUMP_STATE_KEY, null);
		this.intervalMin = configs.getInt(CirculationPumpConst.INTERVAL_KEY, CirculationPumpConst.INTERVAL_DEFAULT)*60000;
		this.stateListener = registerStateListener(stateValue);
		
		this.stackTemperatureMax = configs.getDouble(CirculationPumpConst.MAX_STACK_TEMPERATURE_KEY, CirculationPumpConst.MAX_STACK_TEMPERATURE_DEFAULT);
		this.deltaTemperatureMin = configs.getDouble(CirculationPumpConst.MIN_DELTA_TEMPERATURE_KEY, CirculationPumpConst.MIN_DELTA_TEMPERATURE_DEFAULT);
		this.deltaTemperature = configs.get(CirculationPumpConst.DELTA_TEMPERATURE_KEY, null);
		registerTemperatureListener(configs.get(CirculationPumpConst.STACK_TEMPERATURE_KEY, null), CirculationTemperature.STACK);
		registerTemperatureListener(configs.get(CirculationPumpConst.OUT_TEMPERATURE_KEY, null), CirculationTemperature.OUT);
		registerTemperatureListener(configs.get(CirculationPumpConst.IN_TEMPERATURE_KEY, null), CirculationTemperature.IN);
	}
	
	private ControlValueListener registerStateListener(String id) throws ComponentConfigException {
		if (id != null) {
			ControlValueListener stateListener = new ControlValueListener() {
				
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
	
	private void registerTemperatureListener(String id, CirculationTemperature type) throws ComponentConfigException {
		if (id != null) {
			CirculationTemperatureListener listener = new CirculationTemperatureListener(this, type, id);
			
			temperatureListeners.add(listener);
			control.registerValueListener(id, listener);
		}
		else throw new ComponentConfigException("Unable to find configured cabinet temperature value");
	}
	
	public void deactivate() {
		if (stateListener != null) {
			control.deregisterValueListener(stateValue, stateListener);
		}
		for (CirculationTemperatureListener listener: temperatureListeners) {
			control.deregisterValueListener(listener.getId(), listener);
		}
	}
	
	public void start() {
		control.writeValue(stateValue, new BooleanValue(true));
	}
	
	public void stop() {
		control.writeValue(stateValue, new BooleanValue(false));
	}
	
	@Override
	public synchronized void onTemperatureReceived(CirculationTemperature type, Value temperature) {
		switch(type) {
		case STACK:
			if (!stateValueLast.booleanValue() && temperature.doubleValue() > stackTemperatureMax) {
				start();
			}
			break;
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
				
				if (stateValueLast.booleanValue() && delta <= deltaTemperatureMin && 
						System.currentTimeMillis() - startTimeLast >= intervalMin) {
					
					stop();
				}
			}
		}
	}
}
