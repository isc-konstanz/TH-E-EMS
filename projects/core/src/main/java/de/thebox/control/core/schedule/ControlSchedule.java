package de.thebox.control.core.schedule;

import java.util.HashMap;

import de.thebox.control.core.component.Component;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ScheduleComponent;
import de.thebox.control.core.component.ComponentService;

public class ControlSchedule extends HashMap<String, Schedule> {
	private static final long serialVersionUID = 8892391031173460437L;

	private final long timestamp;

	public ControlSchedule() {
		timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Schedule get(ComponentService component) throws ComponentException {
		if (component instanceof ScheduleComponent) {
			return get((ScheduleComponent) component);
		}
		throw new ComponentException("Component not able to be scheduled");
	}

	public Schedule get(ScheduleComponent component) {
		return get(component.getType());
	}

	public Schedule get(Component type) {
		return get(type.getKey());
	}

	public Schedule getInverterSchedule() {
		return get(Component.INVERTER);
	}

	public Schedule getCogeneratorSchedule() {
		return get(Component.COGENERATOR);
	}

	public Schedule getHeatPumpSchedule() {
		return get(Component.HEATPUMP);
	}

	public Schedule add(Component type, Schedule schedule) {
		return put(type.getKey(), schedule);
	}

	public Schedule addInverterSchedule(Schedule schedule) {
		return add(Component.INVERTER, schedule);
	}

	public Schedule addCogeneratorSchedule(Schedule schedule) {
		return add(Component.COGENERATOR, schedule);
	}

	public Schedule addHeatPumpSchedule(Schedule schedule) {
		return add(Component.HEATPUMP, schedule);
	}

	public boolean contains(ComponentService component) throws ComponentException {
		if (component instanceof ScheduleComponent) {
			return contains((ScheduleComponent) component);
		}
		throw new ComponentException("Component not able to be scheduled");
	}

	public boolean contains(ScheduleComponent component) {
		return contains(component.getType());
	}

	public boolean contains(Component type) {
		return containsKey(type.getKey());
	}

}
