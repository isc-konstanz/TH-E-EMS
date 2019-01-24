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
package org.the.ems.core.data;

import java.util.Objects;

public class ShortValue extends Value {

	private final short value;

	public ShortValue(short value, long timestamp) {
		super(ValueType.SHORT, timestamp);
		this.value = value;
	}

	public ShortValue(short value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public short shortValue() {
		return value;
	}

	@Override
	public byte byteValue() {
		return (byte) value;
	}

	@Override
	public boolean booleanValue() {
		return (value != 0);
	}

	@Override
	public String toString() {
		return Short.toString(value);
	}

	@Override
	public boolean equals(Object o) {

		if (o == this) return true;
		if (!(o instanceof ShortValue)) {
			return false;
		}
		ShortValue user = (ShortValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static ShortValue emptyValue() {
		return new ShortValue((short) 0);
	}

}
