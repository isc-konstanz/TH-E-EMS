package de.thebox.control.opt;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class OptimizationConfig extends ComponentConfig {

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

	public int getInterval() throws ComponentConfigException {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
