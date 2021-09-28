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

import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationCollection<V> {

	final Map<String, V> configs;

	public ConfigurationCollection() {
		configs = new HashMap<String, V>();
	}

	public abstract V add(String key, String value);

	public V add(String key, V value) {
		return configs.put(key, value);
	}

	public boolean contains(String key) {
		return configs.containsKey(key);
	}

	public V get(String key) {
		return configs.get(key);
	}

	public <E extends Enum<E>> V get(E e) {
		return configs.get(e.toString());
	}

	public static class StringCollection extends ConfigurationCollection<String> {

		@Override
		public String add(String key, String value) {
			return add(key, String.valueOf(value));
		}
	}

	public static class DoubleCollection extends ConfigurationCollection<Double> {

		@Override
		public Double add(String key, String value) {
			return add(key, Double.valueOf(value));
		}
	}

	public static class FloatCollection extends ConfigurationCollection<Float> {

		@Override
		public Float add(String key, String value) {
			return add(key, Float.valueOf(value));
		}
	}

	public static class LongCollection extends ConfigurationCollection<Long> {

		@Override
		public Long add(String key, String value) {
			return add(key, Long.valueOf(value));
		}
	}

	public static class IntegerCollection extends ConfigurationCollection<Integer> {

		@Override
		public Integer add(String key, String value) {
			return add(key, Integer.valueOf(value));
		}
	}

	public static class ShortCollection extends ConfigurationCollection<Short> {

		@Override
		public Short add(String key, String value) {
			return add(key, Short.valueOf(value));
		}
	}

	public static class ByteCollection extends ConfigurationCollection<Byte> {

		@Override
		public Byte add(String key, String value) {
			return add(key, Byte.valueOf(value));
		}
	}

	public static class BooleanCollection extends ConfigurationCollection<Boolean> {

		@Override
		public Boolean add(String key, String value) {
			return add(key, Boolean.valueOf(value));
		}
	}

}
