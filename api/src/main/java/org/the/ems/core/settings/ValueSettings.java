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
package org.the.ems.core.settings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ByteValue;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.FloatValue;
import org.the.ems.core.data.IntValue;
import org.the.ems.core.data.LongValue;
import org.the.ems.core.data.ShortValue;
import org.the.ems.core.data.Value;

public class ValueSettings extends StartStopSettings {

	protected final Value value;

	public static ValueSettings ofDouble(double value, long timestamp) {
		return new ValueSettings(new DoubleValue(value, timestamp));
	}

	public static ValueSettings ofFloat(float value, long timestamp) {
		return new ValueSettings(new FloatValue(value, timestamp));
	}

	public static ValueSettings ofLong(long value, long timestamp) {
		return new ValueSettings(new LongValue(value, timestamp));
	}

	public static ValueSettings ofInteger(int value, long timestamp) {
		return new ValueSettings(new IntValue(value, timestamp));
	}

	public static ValueSettings ofShort(short value, long timestamp) {
		return new ValueSettings(new ShortValue(value, timestamp));
	}

	public static ValueSettings ofByte(byte value, long timestamp) {
		return new ValueSettings(new ByteValue(value, timestamp));
	}

	public static ValueSettings ofBoolean(boolean value, long timestamp) {
		return new ValueSettings(new BooleanValue(value, timestamp));
	}

	public ValueSettings(Value value) {
		super();
		this.value = value;
	}

	@Override
	public LocalDateTime getDateTime() {
		return value.getDateTime();
	}

	@Override
	public LocalDate getDate() {
		return value.getDate();
	}

	@Override
	public LocalTime getTime() {
		return value.getTime();
	}

	@Override
	public long getEpochSeconds() {
		return value.getEpochSeconds();
	}

	@Override
	public long getEpochMillis() {
		return value.getEpochMillis();
	}

	public Value getValue() {
		return value;
	}

}
