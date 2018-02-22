package de.thebox.control.core.config;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.ConfigurationException;

public abstract class Configuration {

	protected final Preferences config;

	public Configuration(Preferences config) {
		this.config = config.node(getSectionKey());
	}

	protected abstract String getSectionKey();

	protected String getKey(String key) throws ConfigurationException {
		String value = config.get(key, null);
		if (value != null && !value.isEmpty()) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected double getDouble(String key) throws ConfigurationException {
		Double value = config.getDouble(key, Double.NaN);
		if (value != Double.NaN) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected int getInteger(String key) throws ConfigurationException {
		Integer value = config.getInt(key, Integer.MAX_VALUE);
		if (value != Integer.MAX_VALUE) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

}
