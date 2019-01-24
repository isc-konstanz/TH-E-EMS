package org.the.cmpt.hp.basic;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class BasicConfig extends Configurations {

	public final static String STATE_KEY = "state";

	public final static String INTERVAL_KEY = "interval_min";
	public final static int INTERVAL_DEFAULT = 10;

	public final static String TSS_TEMPERATURE_KEY = "tss_temp";
	public final static String TSS_TEMPERATURE_MIN_KEY = "tss_temp_min";
	public final static String TSS_TEMPERATURE_MAX_KEY = "tss_temp_max";
	public final static String TSS_TEMPERATURE_IN_MAX_KEY = "tss_temp_in_max";

	public String getStateKey() throws ConfigurationException {
		return get(STATE_KEY);
	}

	public int getIntervalMin() {
		return prefs.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

	public String getTemperatureKey() throws ConfigurationException {
		return get(TSS_TEMPERATURE_KEY);
	}

	public double getTemperatureMin() throws ConfigurationException {
		return getDouble(TSS_TEMPERATURE_MIN_KEY);
	}

	public double getTemperatureMax() throws ConfigurationException {
		return getDouble(TSS_TEMPERATURE_MAX_KEY);
	}

	public double getInputTemperatureMax() throws ConfigurationException {
		return getDouble(TSS_TEMPERATURE_IN_MAX_KEY);
	}

}
