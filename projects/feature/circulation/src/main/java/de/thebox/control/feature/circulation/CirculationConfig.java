package de.thebox.control.feature.circulation;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class CirculationConfig extends ComponentConfig {

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

	public String getDeltaTemperature() throws ComponentConfigException {
		return getKey(DELTA_TEMPERATURE_KEY);
	}

	public String getInTemperature() throws ComponentConfigException {
		return getKey(IN_TEMPERATURE_KEY);
	}

	public String getOutTemperature() throws ComponentConfigException {
		return getKey(OUT_TEMPERATURE_KEY);
	}

}
