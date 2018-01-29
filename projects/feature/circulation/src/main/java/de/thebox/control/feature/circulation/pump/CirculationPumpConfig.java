package de.thebox.control.feature.circulation.pump;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.feature.circulation.CirculationConfig;

public class CirculationPumpConfig extends CirculationConfig {

	protected final static String MIN_DELTA_TEMPERATURE_KEY = "circDeltaTempMin";
	protected final static double MIN_DELTA_TEMPERATURE_DEFAULT = 0.5;

	protected final static String MAX_REF_TEMPERATURE_KEY = "referenceTempMax";
	protected final static String MIN_REF_TEMPERATURE_KEY = "referenceTempMin";
	protected final static String REF_TEMPERATURE_KEY = "referenceTemp";

	protected final static String INTERVAL_KEY = "intervalMin";
	protected final static int INTERVAL_DEFAULT = 15;

	protected final static String PUMP_STATE_KEY = "pumpState";

	public CirculationPumpConfig(Preferences prefs) {
		super(prefs);
	}

	public double getDeltaTemperatureMin() {
		return config.getDouble(MIN_DELTA_TEMPERATURE_KEY, MIN_DELTA_TEMPERATURE_DEFAULT);
	}

	public double getReferenceTemperatureMax() throws ComponentConfigException {
		return getDouble(MAX_REF_TEMPERATURE_KEY);
	}

	public double getReferenceTemperatureMin() throws ComponentConfigException {
		return getDouble(MIN_REF_TEMPERATURE_KEY);
	}

	public String getReferenceTemperature() throws ComponentConfigException {
		return getKey(REF_TEMPERATURE_KEY);
	}

	public String getState() throws ComponentConfigException {
		return getKey(PUMP_STATE_KEY);
	}

	public int getIntervalMin() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
