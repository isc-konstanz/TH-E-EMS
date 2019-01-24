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

public class DoubleValue extends Value {

	private final double value;

	public DoubleValue(double value, long timestamp) {
		super(ValueType.DOUBLE, timestamp);
		this.value = value;
	}

	public DoubleValue(double value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return (float) value;
	}

	@Override
	public long longValue() {
		return (long) value;
	}

	@Override
	public int intValue() {
		return (int) value;
	}

	@Override
	public short shortValue() {
		return (short) value;
	}

	@Override
	public byte byteValue() {
		return (byte) value;
	}

	@Override
	public boolean booleanValue() {
		return (value != 0.0);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof DoubleValue)) {
			return false;
		}
		DoubleValue user = (DoubleValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static DoubleValue emptyValue() {
		return new DoubleValue(0);
	}

}
