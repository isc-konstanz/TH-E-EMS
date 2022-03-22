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

public class DoubleValue extends Value {

	public DoubleValue(double value, long timestamp) {
		super(ValueType.DOUBLE, value, timestamp);
	}

	public DoubleValue(double value) {
		this(value, System.currentTimeMillis());
	}

	public boolean isNaN() {
		return Double.isNaN(doubleValue());
	}

	public boolean isFinite() {
		return Double.isFinite(doubleValue());
	}

	public boolean isInfinite() {
		return Double.isInfinite(doubleValue());
	}

	public static DoubleValue emptyValue() {
		return new DoubleValue(Double.NaN);
	}

	public static DoubleValue zeroValue() {
		return new DoubleValue(0);
	}

	public static DoubleValue copy(Value value) {
		return new DoubleValue(value.doubleValue(), value.getEpochMillis());
	}

}
