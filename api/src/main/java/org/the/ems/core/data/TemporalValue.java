package org.the.ems.core.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface TemporalValue {

	public LocalDateTime getDateTime();

	public LocalDate getDate();

	public LocalTime getTime();

	public long getEpochSeconds();

	public long getEpochMillis();

}
