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
package org.the.ems.core.config;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Configurations {
	protected final static String SECTION = "General";

	protected Preferences prefs;

	public Configurations(Preferences prefs) {
		this.prefs = prefs;
	}

	public Configurations() {
	}

	protected String getSection() {
		return SECTION;
	}

    public <P extends Configurations> P getSection(Class<P> type) throws ConfigurationException {
        try {
			P config = type.getConstructor().newInstance();
        	String section = config.getSection();
			if (hasSection(config)) {
				config.setPreferences(this.prefs.node(section));
				
	            return config;
			}
	        throw new ConfigurationException(MessageFormat.format("Unknown section: {0}", section));
			
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new ConfigurationException(MessageFormat.format("Error casting configuration to class \"{0}\": {1}", 
                    type.getSimpleName(), e.getMessage()));
        }
    }

	public <P extends Configurations> boolean hasSection(Class<P> type) throws ConfigurationException {
		try {
			return hasSection(type.getConstructor().newInstance());
			
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new ConfigurationException(MessageFormat.format("Error casting configuration to class \"{0}\": {1}", 
                    type.getSimpleName(), e.getMessage()));
        }
	}

	public boolean hasSection(Configurations config) throws ConfigurationException {
		try {
			return prefs.nodeExists(config.getSection());
			
		} catch (BackingStoreException e) {
	        throw new ConfigurationException(MessageFormat.format("Error reading configuration \"{0}\": {1}", 
	        		config.getClass().getSimpleName(), e.getMessage()));
		}
	}

    protected void setPreferences(Preferences prefs) {
    	this.prefs = prefs;
    }

    protected boolean contains(String key) {
		String value = prefs.get(key, null);
		return value != null && !value.isEmpty();
    }

	protected String get(String key) throws ConfigurationException {
		String value = prefs.get(key, null);
		if (value != null && !value.isEmpty()) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected String get(String key, String def) {
		return prefs.get(key, def);
	}

	protected double getDouble(String key) throws ConfigurationException {
		Double value = prefs.getDouble(key, Double.NaN);
		if (value != Double.NaN) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected double getDouble(String key, double def) {
		return prefs.getDouble(key, def);
	}

	protected int getInteger(String key) throws ConfigurationException {
		Integer value = prefs.getInt(key, Integer.MAX_VALUE);
		if (value != Integer.MAX_VALUE) {
			return value;
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected int getInteger(String key, int def) {
		return prefs.getInt(key, def);
	}

	protected boolean getBoolean(String key) throws ConfigurationException {
		if (prefs.get(key, null) != null) {
			return prefs.getBoolean(key, false);
		}
		throw new ConfigurationException("Parameter \"" + key + "\" missing");
	}

	protected boolean getBoolean(String key, boolean def) {
		return prefs.getBoolean(key, def);
	}

}
