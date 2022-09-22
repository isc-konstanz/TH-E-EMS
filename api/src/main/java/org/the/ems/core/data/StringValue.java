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

public class StringValue extends Value {

	protected final String value;

	public StringValue(String value, long timestamp) {
		super(ValueType.STRING, timestamp);
		this.value = value;
	}

	public StringValue(String value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public String stringValue() {
		return this.value;
	}

    @Override
    public double doubleValue() throws NumberFormatException {
        return Double.parseDouble(value);
    }

    @Override
    public float floatValue() throws NumberFormatException {
        return Float.parseFloat(value);
    }

    @Override
    public long longValue() throws NumberFormatException {
        return Long.parseLong(value);
    }

    @Override
    public int intValue() throws NumberFormatException {
        return Integer.parseInt(value);
    }

    @Override
    public short shortValue() throws NumberFormatException {
        return Short.parseShort(value);
    }

    @Override
    public byte byteValue() throws NumberFormatException {
        return Byte.parseByte(value);
    }

    @Override
    public boolean booleanValue() {
        return Boolean.parseBoolean(value);
    }

	@Override
	public int hashCode() {
		return Objects.hash(type, instant, value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof StringValue)) {
			return false;
		}
		StringValue v = (StringValue) o;
		return type == v.type &&
				Objects.equals(instant, v.instant) &&
				Objects.equals(value, v.value);
	}

}
