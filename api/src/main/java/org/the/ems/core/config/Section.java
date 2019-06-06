/* 
 * Copyright 2016-19 ISC Konstanz
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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Section extends HashMap<String, String> {
	private static final long serialVersionUID = 2011937687880522916L;

	public final static String DISABLED = "disabled";

	Section() {
	}

	Section(Map<String, String> values) {
		putAll(values);
	}

	Section(Set<Entry<String, String>> entries) {
		for (Entry<String, String> entry : entries) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public boolean contains(String key) {
		String value = get(key);
		if (value == null || value.isEmpty()) {
			return false;
		}
		return true;
	}

	public Boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}

	public Byte getByte(String key) {
		return Byte.parseByte(get(key));
	}

	public Short getShort(String key) {
		return Short.parseShort(get(key));
	}

	public Integer getInteger(String key) {
		return Integer.parseInt(get(key));
	}

	public Long getLong(String key) {
		return Long.parseLong(get(key));
	}

	public Float getFloat(String key) {
		return Float.parseFloat(get(key));
	}

	public Double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

	public InetAddress getInetAddress(String key) throws ConfigurationException {
		try {
			return InetAddress.getByName(get(key));
		} catch (UnknownHostException e) {
			throw newSyntaxException(key, InetAddress.class.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key, Class<T> type) throws ConfigurationException, NoSuchFieldException {
		Method method = null;
		try {
			method = type.getMethod("valueOf", String.class);
			
		} catch (NoSuchMethodException | SecurityException e) {
			// check if method is null and procees
		}
		if (method == null) {
			throw new NoSuchFieldException(
					type + " Driver implementation error not supported data type. Report component developer");
		}
		
		try {
			if (type.isEnum()) {
				return (T) method.invoke(null, get(key).toUpperCase());
			}
			return (T) method.invoke(null, get(key));
			
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw newSyntaxException(key, type.getSimpleName());
		}
	}

	public Boolean isDisabled() {
		if (!contains(Section.DISABLED)) {
			return false;
		}
		return getBoolean(Section.DISABLED);
	}

	private ConfigurationException newSyntaxException(String key, String type) {
		return new ConfigurationException(MessageFormat.format("Value of {0} is not type of {1}.", 
				key, type));
	}

}
