package de.thebox.control.opt;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class OptimizationConfig extends Configuration {

	public final static String SECTION = "Optimization";

	public final static String INTERVAL_KEY = "interval";
	public final static int INTERVAL_DEFAULT = 15;

	public OptimizationConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public int getInterval() throws ConfigurationException {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
