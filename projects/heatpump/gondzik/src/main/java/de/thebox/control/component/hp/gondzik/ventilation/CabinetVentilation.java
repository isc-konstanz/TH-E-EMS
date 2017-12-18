package de.thebox.control.component.hp.gondzik.ventilation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.hp.gondzik.ventilation.CabinetTemperatureListener.CabinetTemperatureCallbacks;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.ControlValueListener;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Value;


public class CabinetVentilation implements CabinetTemperatureCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(CabinetVentilation.class);
	
	private final ControlService control;
	
	private final String stateValue;
	private final ControlValueListener stateListener;
	
	private Value stateValueLast = null;
	private volatile long startTimeLast = 0;
	private final int intervalMin;
	
	private final double temperatureMax;
	private final double temperatureTolerance;
	private final List<CabinetTemperatureListener> temperatureListeners = new ArrayList<CabinetTemperatureListener>();
	private final List<CabinetTemperature> temperatureHighFlags = new ArrayList<CabinetTemperature>(CabinetTemperature.values().length);
	
	public CabinetVentilation(ControlService control, Preferences configs) throws ComponentConfigException {
		this.control = control;
		
		logger.info("Activating TH-E Control cabinet ventilation management");
		
		this.stateValue = configs.get(CabinetVentilationConst.VEMNTILATION_STATE_KEY, null);
		this.intervalMin = configs.getInt(CabinetVentilationConst.INTERVAL_KEY, CabinetVentilationConst.INTERVAL_DEFAULT)*60000;
		this.stateListener = registerStateListener(stateValue);
		
		this.temperatureMax = configs.getDouble(CabinetVentilationConst.MAX_TEMPERATURE_KEY, CabinetVentilationConst.MAX_TEMPERATURE_DEFAULT);
		this.temperatureTolerance = configs.getDouble(CabinetVentilationConst.TOLERANCE_KEY, CabinetVentilationConst.TOLERANCE_DEFAULT);
		registerTemperatureListener(configs.get(CabinetVentilationConst.TOP_TEMPERATURE_KEY, null), CabinetTemperature.TOP);
		registerTemperatureListener(configs.get(CabinetVentilationConst.CENTER_TEMPERATURE_KEY, null), CabinetTemperature.CENTER);
		registerTemperatureListener(configs.get(CabinetVentilationConst.BOTTOM_TEMPERATURE_KEY, null), CabinetTemperature.BOTTOM);
	}
	
	private ControlValueListener registerStateListener(String id) throws ComponentConfigException {
		if (id != null) {
			ControlValueListener stateListener = new ControlValueListener() {
				
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
			control.registerValueListener(id, stateListener);
			
			return stateListener;
		}
		throw new ComponentConfigException("Unable to find configured state value");
	}
	
	private void registerTemperatureListener(String id, CabinetTemperature type) throws ComponentConfigException {
		if (id != null) {
			CabinetTemperatureListener listener = new CabinetTemperatureListener(this, type, id);
			
			temperatureListeners.add(listener);
			control.registerValueListener(id, listener);
		}
		else throw new ComponentConfigException("Unable to find configured cabinet temperature value");
	}
	
	public void deactivate() {
		if (stateListener != null) {
			control.deregisterValueListener(stateValue, stateListener);
		}
		for (CabinetTemperatureListener listener: temperatureListeners) {
			control.deregisterValueListener(listener.getId(), listener);
		}
	}
	
	public void start() {
		control.setValue(stateValue, new BooleanValue(true));
	}
	
	public void stop() {
		control.setValue(stateValue, new BooleanValue(false));
	}
	
	@Override
	public synchronized void onTemperatureReceived(CabinetTemperature type, Double temperature) {
		if (temperature > temperatureMax + temperatureTolerance && !temperatureHighFlags.contains(type)) {
			temperatureHighFlags.add(type);
			
			start();
		}
		else if (temperature < temperatureMax - temperatureTolerance && temperatureHighFlags.contains(type) &&
				System.currentTimeMillis() - startTimeLast >= intervalMin) {
			
			temperatureHighFlags.remove(type);
			if (temperatureHighFlags.size() == 0) {
				stop();
			}
		}
	}
}
