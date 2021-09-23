/* 
 * Copyright 2016-21 ISC Konstanz
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
package org.the.ems.core.data;

import java.util.Objects;

public abstract class Value {

	protected final ValueType type;

	protected final Number value;
	protected final long time;

	public Value(ValueType type, Number value, long timestamp) {
		this.value = value;
		this.time = timestamp;
		this.type = type;
	}

	public ValueType getType() {
		return type;
	}

	public long getTime() {
		return time;
	}

	public double doubleValue() {
		return this.value.doubleValue();
	}

	public float floatValue() {
		return this.value.floatValue();
	}

	public long longValue() {
		return this.value.longValue();
	}

	public int intValue() {
		return this.value.intValue();
	}

	public short shortValue() {
		return this.value.shortValue();
	}

	public byte byteValue() {
		return this.value.byteValue();
	}

	public boolean booleanValue() {
		return this.value.intValue() != 0;
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof Value)) {
			return false;
		}
		Value eq = (Value) o;
		return type == eq.type &&
				Objects.equals(time, eq.time) &&
				Objects.equals(value, eq.value);
	}

}
