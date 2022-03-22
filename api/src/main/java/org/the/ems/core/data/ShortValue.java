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

public class ShortValue extends Value {

	public ShortValue(short value, long timestamp) {
		super(ValueType.SHORT, value, timestamp);
	}

	public ShortValue(short value) {
		this(value, System.currentTimeMillis());
	}

	public static ShortValue zeroValue() {
		return new ShortValue((short) 0);
	}

	public static ShortValue copy(Value value) {
		return new ShortValue(value.shortValue(), value.getEpochMillis());
	}

}
