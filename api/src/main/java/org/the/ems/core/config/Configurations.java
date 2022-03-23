/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Configurations extends Hashtable<String, Object> {
	private static final long serialVersionUID = 7726428276708545480L;

	public final static String GENERAL = "general";
	public final static String ENABLED = "enabled";
	public final static String DISABLED = "disabled";

	protected Configurations() {
	}

	protected Configurations(Map<String, Object> configs) {
		put(configs);
	}

	protected void put(Map<String, Object> configs) {
		put(configs.entrySet());
	}

	private void put(Set<Entry<String, Object>> configs) {
		for (Entry<String, Object> entry : configs) {
			put(entry);
		}
	}

	private void put(Entry<String, Object> entry) {
		put(entry.getKey(), entry.getValue());
	}

	public Object put(String section, String key, Object value) {
		return put(parseSectionKey(section, key), value);
	}

	public Object put(String section, String key, String value) {
		return put(parseSectionKey(section, key), value);
	}

	public boolean containsKey(String section, String key) {
		return containsKey(parseSectionKey(section, key));
	}

	public boolean containsSection(String section) {
		String parsedSection = parseSection(section);
		return keySet().stream().anyMatch(s -> s.startsWith(parsedSection));
	}

	public String get(String section, String key) {
		return String.valueOf(get(parseSectionKey(section, key)));
	}

	public Boolean getBoolean(String section, String key) {
		return Boolean.parseBoolean(get(section, key));
	}

	public Byte getByte(String section, String key) {
		return Byte.parseByte(get(section, key));
	}

	public Short getShort(String section, String key) {
		return Short.parseShort(get(section, key));
	}

	public Integer getInteger(String section, String key) {
		return Integer.parseInt(get(section, key));
	}

	public Long getLong(String section, String key) {
		return Long.parseLong(get(section, key));
	}

	public Float getFloat(String section, String key) {
		return Float.parseFloat(get(section, key));
	}

	public Double getDouble(String section, String key) {
		return Double.parseDouble(get(section, key));
	}

	public InetAddress getInetAddress(String section, String key) throws ConfigurationException {
		try {
			return InetAddress.getByName(get(section, key));
		} catch (UnknownHostException e) {
			throw newSyntaxException(key, InetAddress.class.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String section, String key, Class<T> type)
			throws ConfigurationException, NoSuchFieldException {
		
		if (type.isAssignableFrom(boolean.class) || type.isAssignableFrom(Boolean.class)) {
			return (T) getBoolean(section, key);
		}
		else if (type.isAssignableFrom(byte.class) || type.isAssignableFrom(Byte.class)) {
			return (T) getByte(section, key);
		}
		else if (type.isAssignableFrom(short.class) || type.isAssignableFrom(Short.class)) {
			return (T) getShort(section, key);
		}
		else if (type.isAssignableFrom(int.class) || type.isAssignableFrom(Integer.class)) {
			return (T) getInteger(section, key);
		}
		else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
			return (T) getLong(section, key);
		}
		else if (type.isAssignableFrom(float.class) || type.isAssignableFrom(Float.class)) {
			return (T) getFloat(section, key);
		}
		else if (type.isAssignableFrom(double.class) || type.isAssignableFrom(Double.class)) {
			return (T) getDouble(section, key);
		}
		else if (type.isAssignableFrom(String.class)) {
			return (T) get(section, key);
		}
		else if (type.isAssignableFrom(InetAddress.class)) {
			return (T) getInetAddress(section, key);
		}
		else {
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
					return (T) method.invoke(null, get(section, key).toUpperCase());
				}
				return (T) method.invoke(null, get(section, key));
				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw newSyntaxException(key, type.getSimpleName());
			}
		}
	}

	public List<String> search(String section, String pattern) {
		List<String> result = new ArrayList<String>();
		for (Entry<String, Object> entry : entrySet()) {
			if (entry.getKey().matches(parseSectionKey(section, pattern).replace("?", ".?").replace("*", ".*?"))) {
				result.add(entry.getKey().substring(parseSection(section).length()+1));
			}
		}
		return result;
	}

	public Boolean isEnabled(String section) {
		if (!containsSection(section)) {
			return false;
		}
		else if (containsKey(section, DISABLED)) {
			return !getBoolean(section, DISABLED);
		}
		else if (containsKey(section, ENABLED)) {
			return getBoolean(section, ENABLED);
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("Configurations:").append(System.lineSeparator());
		for (Entry<String, Object> entry : entrySet()) {
			str.append(entry.getKey()).append("=");
			str.append(entry.getValue()).append(System.lineSeparator());
		}
		return str.toString();
	}

	private ConfigurationException newSyntaxException(String key, String type) {
		return new ConfigurationException(MessageFormat.format("Value of {0} is not type of {1}.", 
				key, type));
	}

	private static String parseSection(String section) {
		//String packageName = Configurations.class.getPackage().getName().replace(".core.config", "").toLowerCase();
		StringBuilder result = new StringBuilder()
				//.append(packageName).append(".")
				.append(section.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_"));

		return result.toString();
	}

	private static String parseSectionKey(String section, String key) {
		StringBuilder result = new StringBuilder()
				.append(parseSection(section)).append(".")
				.append(key.toLowerCase());
		
		return result.toString();
	}

	@Override
	public Configurations clone() {
		return (Configurations) super.clone();
	}

	@SuppressWarnings("unchecked")
	public static Configurations create(Map<String, ?> properties) {
		return new Configurations((Map<String, Object>) properties);
	}

	public static Configurations create() {
		return new Configurations();
	}

}
