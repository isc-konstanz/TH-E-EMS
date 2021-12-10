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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.the.ems.core.HeatingMode;
import org.the.ems.core.data.Value;

public class HeatingSettings extends StartStopSettings {

	protected final Instant instant;
	protected final HeatingMode mode;

	public static HeatingSettings newHeatingWaterHeating(long timestamp) {
		return new HeatingSettings(HeatingMode.HEATING_WATER, timestamp);
	}

	public static HeatingSettings newDomesticWaterHeating(long timestamp) {
		return new HeatingSettings(HeatingMode.DOMESTIC_WATER, timestamp);
	}

	public HeatingSettings(HeatingMode mode, long timestamp) {
		super();
		this.mode = mode;

		// TODO: Add additional constructors and start refactoring to work with time library instead of timestamps
		this.instant = Instant.ofEpochMilli(timestamp);
	}

	@Override
	public LocalDateTime getDateTime() {
		return LocalDateTime.ofInstant(instant, Value.ZONE);
	}

	@Override
	public LocalDate getDate() {
		return LocalDate.ofInstant(instant, Value.ZONE);
	}

	@Override
	public LocalTime getTime() {
		return LocalTime.ofInstant(instant, Value.ZONE);
	}

	@Override
	public long getEpochSeconds() {
		return instant.getEpochSecond();
	}

	@Override
	public long getEpochMillis() { //throws ArithmeticException {
		return instant.toEpochMilli();
	}

	public HeatingMode getMode() {
		return mode;
	}

}
