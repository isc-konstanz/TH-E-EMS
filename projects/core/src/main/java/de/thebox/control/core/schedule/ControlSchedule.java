package de.thebox.control.core.schedule;

import java.util.HashMap;

public class ControlSchedule extends HashMap<String, Schedule> {
	private static final long serialVersionUID = 8892391031173460437L;

	public final static String HEATPUMP = "hp";
	public final static String COGENERATOR = "chp";

	private final long timestamp;

	public ControlSchedule() {
		timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Schedule getHeatPumpSchedule() {
		return get(HEATPUMP);
	}

	public Schedule getCogeneratorSchedule() {
		return get(COGENERATOR);
	}

}
