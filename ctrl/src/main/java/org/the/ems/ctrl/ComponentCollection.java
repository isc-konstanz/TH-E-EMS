package org.the.ems.ctrl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.the.ems.core.ComponentType;

abstract class ComponentCollection<C extends ComponentControl<?>> extends LinkedHashMap<String, C> {
	private static final long serialVersionUID = -1170124420619031062L;

	public void add(C component) {
		String id = component.getId();
		put(id, component);
	}

	public List<C> getAll(ComponentType... types) {
		List<C> components = new ArrayList<C>();
		for (ComponentType type : types) {
			for (C appliance : values()) {
				if (appliance.getType() == type) {
					components.add(appliance);
				}
			}
		}
		return components;
	}

}
