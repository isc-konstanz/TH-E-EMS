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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Configurations {

	private final Map<String, Section> sections = new HashMap<String, Section>();

	public void add(String section, String key, Object value) {
		add(section, key, String.valueOf(value));
	}

	public void add(String section, String key, String value) {
		if (!sections.containsKey(section)) {
			sections.put(section, new Section());
		}
		sections.get(section).put(key, value);
	}

	public void add(String section, Map<String, String> configs) {
		sections.put(section, new Section(configs));
	}

	public void add(String section, Set<Entry<String, String>> configs) {
		sections.put(section, new Section(configs));
	}

	public boolean contains(String section, String key) {
		if (!sections.containsKey(section)) {
			return false;
		}
		return sections.get(section).contains(key);
	}

	public Boolean getBoolean(String section, String key) {
		return sections.get(section).getBoolean(key);
	}

	public Byte getByte(String section, String key) {
		return sections.get(section).getByte(key);
	}

	public Short getShort(String section, String key) {
		return sections.get(section).getShort(key);
	}

	public Integer getInteger(String section, String key) {
		return sections.get(section).getInteger(key);
	}

	public Long getLong(String section, String key) {
		return sections.get(section).getLong(key);
	}

	public Float getFloat(String section, String key) {
		return sections.get(section).getFloat(key);
	}

	public Double getDouble(String section, String key) {
		return sections.get(section).getDouble(key);
	}

	public String getString(String section, String key) {
		return sections.get(section).get(key);
	}

	public InetAddress getInetAddress(String section, String key) throws ConfigurationException {
		return sections.get(section).getInetAddress(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String section, String key, Class<T> type)
			throws ConfigurationException, IllegalArgumentException, NoSuchFieldException {
		
		if (type.isAssignableFrom(boolean.class) || type.isAssignableFrom(Boolean.class)) {
			return (T) sections.get(section).getBoolean(key);
		}
		else if (type.isAssignableFrom(byte.class) || type.isAssignableFrom(Byte.class)) {
			return (T) sections.get(section).getByte(key);
		}
		else if (type.isAssignableFrom(short.class) || type.isAssignableFrom(Short.class)) {
			return (T) sections.get(section).getShort(key);
		}
		else if (type.isAssignableFrom(int.class) || type.isAssignableFrom(Integer.class)) {
			return (T) sections.get(section).getInteger(key);
		}
		else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
			return (T) sections.get(section).getLong(key);
		}
		else if (type.isAssignableFrom(float.class) || type.isAssignableFrom(Float.class)) {
			return (T) sections.get(section).getFloat(key);
		}
		else if (type.isAssignableFrom(double.class) || type.isAssignableFrom(Double.class)) {
			return (T) sections.get(section).getDouble(key);
		}
		else if (type.isAssignableFrom(String.class)) {
			return (T) sections.get(section).get(key);
		}
		else if (type.isAssignableFrom(InetAddress.class)) {
			return (T) sections.get(section).getInetAddress(key);
		}
		else {
			return sections.get(section).get(key, type);
		}
	}

	public Boolean isDisabled(String section) {
		if (!sections.containsKey(section)) {
			return true;
		}
		return sections.get(section).isDisabled();
	}

	public static String parse(String key) {
		LinkedList<String> result = new LinkedList<String>();
		for (String str : key.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
			result.add(str.toLowerCase());
		}
		return String.join("_", result);
	}

}
