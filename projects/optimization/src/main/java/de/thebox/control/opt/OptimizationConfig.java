package de.thebox.control.opt;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class OptimizationConfig extends Configuration {

	public final static String SECTION = "Optimization";

	public final static String INTERVAL_KEY = "interval";
	public final static int INTERVAL_DEFAULT = 15;

	public final static String PYTHON_KEY = "python";
	public final static String PYTHON_DEFAULT = "python";

	public final static String SCRIPT_KEY = "script";
	public final static String SCRIPT_DEFAULT = "th-e-optimization";


	public OptimizationConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public int getInterval() throws ConfigurationException {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT);
	}

	public String getPython() {
		return config.get(PYTHON_KEY, PYTHON_DEFAULT);
	}

	public String getScript() {
		return config.get(SCRIPT_KEY, SCRIPT_DEFAULT);
	}

}
