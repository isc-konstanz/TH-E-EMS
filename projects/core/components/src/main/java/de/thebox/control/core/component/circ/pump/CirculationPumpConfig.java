package de.thebox.control.core.component.circ.pump;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CirculationPumpConfig extends Configuration {

	protected final static String SECTION = "Circulation";

	protected final static String MIN_DELTA_TEMPERATURE_KEY = "circDeltaTempMin";
	protected final static double MIN_DELTA_TEMPERATURE_DEFAULT = 0.5;

	protected final static String MAX_REF_TEMPERATURE_KEY = "referenceTempMax";
	protected final static String MIN_REF_TEMPERATURE_KEY = "referenceTempMin";
	protected final static String REF_TEMPERATURE_KEY = "referenceTemp";

	protected final static String INTERVAL_KEY = "intervalMin";
	protected final static int INTERVAL_DEFAULT = 15;

	protected final static String PUMP_STATE_KEY = "pumpState";

	public CirculationPumpConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public boolean isEnabled() {
		String value = config.get(PUMP_STATE_KEY, null);
		if (value != null && !value.isEmpty()) {
			return true;
		}
		return false;
	}

	public double getDeltaTemperatureMin() {
		return config.getDouble(MIN_DELTA_TEMPERATURE_KEY, MIN_DELTA_TEMPERATURE_DEFAULT);
	}

	public double getReferenceTemperatureMax() throws ConfigurationException {
		return getDouble(MAX_REF_TEMPERATURE_KEY);
	}

	public double getReferenceTemperatureMin() throws ConfigurationException {
		return getDouble(MIN_REF_TEMPERATURE_KEY);
	}

	public String getReferenceTemperature() throws ConfigurationException {
		return getKey(REF_TEMPERATURE_KEY);
	}

	public String getState() throws ConfigurationException {
		return getKey(PUMP_STATE_KEY);
	}

	public int getIntervalMin() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
