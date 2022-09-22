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

public class FloatValue extends NumberValue {

	public FloatValue(float value, long timestamp) {
		super(ValueType.FLOAT, value, timestamp);
	}

	public FloatValue(float value) {
		this(value, System.currentTimeMillis());
	}

	public boolean isNaN() {
		return Float.isNaN(floatValue());
	}

	public boolean isFinite() {
		return Float.isFinite(floatValue());
	}

	public boolean isInfinite() {
		return Float.isInfinite(floatValue());
	}

	public boolean equals(float value) {
		return floatValue() != value;
	}

	public static FloatValue emptyValue() {
		return new FloatValue(Float.NaN);
	}

	public static FloatValue zeroValue() {
		return new FloatValue(0);
	}

	public static FloatValue copy(Value value) {
		return new FloatValue(value.floatValue(), value.getEpochMillis());
	}

}
