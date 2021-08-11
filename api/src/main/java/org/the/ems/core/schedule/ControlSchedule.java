/* 
 * Copyright 2016-21 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
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
package org.the.ems.core.schedule;

import java.util.HashMap;

import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;

public class ControlSchedule extends HashMap<String, Schedule> {
	private static final long serialVersionUID = 8892391031173460437L;

	private final long timestamp;

	public ControlSchedule() {
		timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Schedule get(ComponentService component) {
		return get(component.getType());
	}

	public Schedule get(ComponentType type) {
		return get(type.getKey());
	}

	public Schedule getInverterSchedule() {
		return get(ComponentType.INVERTER);
	}

	public Schedule getCogeneratorSchedule() {
		return get(ComponentType.COMBINED_HEAT_POWER);
	}

	public Schedule getHeatPumpSchedule() {
		return get(ComponentType.HEAT_PUMP);
	}

	public Schedule add(ComponentType type, Schedule schedule) {
		return put(type.getKey(), schedule);
	}

	public Schedule addInverterSchedule(Schedule schedule) {
		return add(ComponentType.INVERTER, schedule);
	}

	public Schedule addCogeneratorSchedule(Schedule schedule) {
		return add(ComponentType.COMBINED_HEAT_POWER, schedule);
	}

	public Schedule addHeatPumpSchedule(Schedule schedule) {
		return add(ComponentType.HEAT_PUMP, schedule);
	}

	public boolean contains(ComponentService component) {
		return contains(component.getType());
	}

	public boolean contains(ComponentType type) {
		return containsKey(type.getKey());
	}

}
