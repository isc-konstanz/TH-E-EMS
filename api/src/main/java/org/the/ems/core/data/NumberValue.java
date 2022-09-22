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
package org.the.ems.core.data;

import java.util.Objects;

public abstract class NumberValue extends Value {

	protected final Number value;

	public NumberValue(ValueType type, Number value, long timestamp) {
		super(type, timestamp);
		this.value = value;
	}

	@Override
	public String stringValue() {
		return this.value.toString();
	}

	@Override
	public double doubleValue() {
		return this.value.doubleValue();
	}

	@Override
	public float floatValue() {
		return this.value.floatValue();
	}

	@Override
	public long longValue() {
		return this.value.longValue();
	}

	@Override
	public int intValue() {
		return this.value.intValue();
	}

	@Override
	public short shortValue() {
		return this.value.shortValue();
	}

	@Override
	public byte byteValue() {
		return this.value.byteValue();
	}

	@Override
	public boolean booleanValue() {
		return this.value.intValue() != 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, instant, value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof NumberValue)) {
			return false;
		}
		NumberValue v = (NumberValue) o;
		return type == v.type &&
				Objects.equals(instant, v.instant) &&
				Objects.equals(value, v.value);
	}

}
