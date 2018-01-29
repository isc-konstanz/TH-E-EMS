package de.thebox.control.core.component;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfigException;

public abstract class ComponentConfig {

	protected final Preferences config;

	public ComponentConfig(Preferences config) {
		this.config = config.node(getSectionKey());
	}

	protected abstract String getSectionKey();

	protected String getKey(String key) throws ComponentConfigException {
		String value = config.get(key, null);
		if (value != null && !value.isEmpty()) {
			return value;
		}
		throw new ComponentConfigException("Circulation configuration missing: " + key);
	}

	protected double getDouble(String key) throws ComponentConfigException {
		Double value = config.getDouble(key, Double.NaN);
		if (value != Double.NaN) {
			return value;
		}
		throw new ComponentConfigException("Circulation configuration missing: " + key);
	}

	protected int getInteger(String key) throws ComponentConfigException {
		Integer value = config.getInt(key, Integer.MAX_VALUE);
		if (value != Integer.MAX_VALUE) {
			return value;
		}
		throw new ComponentConfigException("Circulation configuration missing: " + key);
	}

}
