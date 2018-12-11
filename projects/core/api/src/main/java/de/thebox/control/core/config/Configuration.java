/* 
 * Copyright 2016-18 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
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

	protected boolean getBoolean(String key) throws ConfigurationException {
		if (config.get(key, null) != null) {
			return config.getBoolean(key, false);
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

}
