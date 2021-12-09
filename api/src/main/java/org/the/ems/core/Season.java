package org.the.ems.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;

public enum Season {
	SPRING,
	SUMMER,
	AUTUMN,
	WINTER;

    public static Season valueOf(final Instant instant) {
    	LocalDate date = LocalDate.from(instant);
    	return valueOf(date.getMonth());
    }

    public static Season valueOf(final Month month) {
    	// Return the meteorological seasons
        switch (month) {
        case MARCH:
        case APRIL:
        case MAY:
            return Season.SPRING;
        case JUNE:
        case JULY:
        case AUGUST:
            return Season.SUMMER;
        case SEPTEMBER:
        case OCTOBER:
        case NOVEMBER:
            return Season.AUTUMN;
        case DECEMBER:
        case JANUARY:
        case FEBRUARY:
            return Season.WINTER;
        }
        throw new IllegalArgumentException("Unknown month: " + month);
    }

}
