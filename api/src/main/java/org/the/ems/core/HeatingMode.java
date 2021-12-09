package org.the.ems.core;

public enum HeatingMode {
	DOMESTIC_WATER,
	HEATING_WATER;

	public String getFullName() {
		switch (this) {
		case DOMESTIC_WATER:
			return "Domestic water";
		case HEATING_WATER:
			return "Heating water";
		default:
			throw new IllegalArgumentException("");
		}
	}
}
