/* 
 * Copyright 2016-20 ISC Konstanz
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
package org.the.ems.core;

import java.util.HashMap;
import java.util.Map;

public enum ComponentType {
	GENERAL("cmpt", "Component"),
	CONTROL("ctrl", "Control"),
	INVERTER("inv", "Inverter"),
	ELECTRICAL_ENERGY_STORAGE("ees", "Electrical Energy Storage"),
	THERMAL_ENERGY_STORAGE("tes", "Thermal Energy Storage"),
	ELECTRIC_VEHICLE("ev", "Electric Vehicle"),
	COMBINED_HEAT_POWER("chp", "Cogenerator"),
	HEAT_PUMP("hp", "Heat Pump"),
	HEATING_ROD("hr", "Heating Rod"),
	VENTILATION("vnt", "Ventilation"),
	APPLIANCE("apl", "Appliance");

	private static final Map<String, ComponentType> keys = new HashMap<String, ComponentType>();

	private final String key;
	private final String name;

	private ComponentType(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public String getId() {
		String id = "org.the.ems";
		switch(this) {
		case GENERAL:
		case CONTROL:
			break;
		default:
			id += ".cmpt";
		}
		return id.concat(".").concat(key);
	}

	public String getKey() {
		return key;
	}

	public String getFullName() {
		return name;
	}

	public ComponentType typeOf(String key) throws IllegalArgumentException {
		ComponentType enumInstance = keys.get(key);
		if (enumInstance == null) {
			throw new IllegalArgumentException("Unknown component type: " + key);
		}
		return enumInstance;
	}

	static {
		for (ComponentType t : ComponentType.values()) {
			if (keys.put(t.key, t) != null) {
				throw new IllegalArgumentException("Duplicate type: " + t.key);
			}
		}
	}

}
