package org.the.cmpt.ctrl.mpc;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class ControlConfig extends Configurations {

	public final static String INTERVAL_KEY = "interval";
	public final static int INTERVAL_DEFAULT = 15;

	public final static String PYTHON_KEY = "python";
	public final static String PYTHON_DEFAULT = "python";

	public final static String SCRIPT_KEY = "script";
	public final static String SCRIPT_DEFAULT = "th-e-optimization";

	public int getInterval() throws ConfigurationException {
		return getInteger(INTERVAL_KEY, INTERVAL_DEFAULT);
	}

	public String getPython() {
		return get(PYTHON_KEY, PYTHON_DEFAULT);
	}

	public String getScript() {
		return get(SCRIPT_KEY, SCRIPT_DEFAULT);
	}

}
