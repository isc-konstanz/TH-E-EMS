package de.thebox.control.core.component.circ;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CirculationConfig extends Configuration {

	protected final static String SECTION = "Circulation";

	protected final static String DELTA_TEMPERATURE_KEY = "circDeltaTemp";
	protected final static String OUT_TEMPERATURE_KEY = "circOutTemp";
	protected final static String IN_TEMPERATURE_KEY = "circInTemp";

	public CirculationConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getDeltaTemperature() throws ConfigurationException {
		return getKey(DELTA_TEMPERATURE_KEY);
	}

	public String getInTemperature() throws ConfigurationException {
		return getKey(IN_TEMPERATURE_KEY);
	}

	public String getOutTemperature() throws ConfigurationException {
		return getKey(OUT_TEMPERATURE_KEY);
	}

}
