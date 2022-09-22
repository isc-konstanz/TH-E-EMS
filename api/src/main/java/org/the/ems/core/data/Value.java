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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public abstract class Value implements TemporalValue {

	// TODO: Make configurable
	public final static ZoneId ZONE = ZoneId.systemDefault();

	protected final Instant instant;

	protected final ValueType type;

	public Value(ValueType type, long timestamp) {
		this.type = type;
		
		// TODO: Add additional constructors and start refactoring to work with time library instead of timestamps
		this.instant = Instant.ofEpochMilli(timestamp);
	}

	public ValueType getType() {
		return type;
	}

	@Override
	public LocalDateTime getDateTime() {
		return LocalDateTime.ofInstant(instant, ZONE);
	}

	@Override
	public LocalDate getDate() {
		return LocalDate.ofInstant(instant, ZONE);
	}

	@Override
	public LocalTime getTime() {
		return LocalTime.ofInstant(instant, ZONE);
	}

	@Override
	public long getEpochSeconds() {
		return instant.getEpochSecond();
	}

	@Override
	public long getEpochMillis() { //throws ArithmeticException {
		return instant.toEpochMilli();
	}

	public abstract String stringValue();

	public abstract double doubleValue();

	public abstract float floatValue();

	public abstract long longValue();

	public abstract int intValue();

	public abstract short shortValue();

	public abstract byte byteValue();

	public abstract boolean booleanValue();

	@Override
	public String toString() {
        return stringValue();
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, instant);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Value)) {
			return false;
		}
		Value v = (Value) o;
		return type == v.type &&
				Objects.equals(instant, v.instant);
	}

}
