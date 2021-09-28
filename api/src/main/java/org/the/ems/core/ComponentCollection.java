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
package org.the.ems.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentCollection extends HashMap<String, ComponentService> {
	private static final long serialVersionUID = 7051113309181699195L;

	private final static Logger logger = LoggerFactory.getLogger(ComponentCollection.class);

	public ComponentService add(ComponentService component) {
		return put(component.getId(), component);
	}

	public void addAll(List<ComponentService> components) {
		for (ComponentService component : components) {
			put(component.getId(), component);
		}
	}

	@SuppressWarnings("unchecked")
	public <C extends ComponentService> List<C> getAll(Class<C> type) {
		List<C> components = new ArrayList<C>();
		for (ComponentService component : values()) {
			if (component.getClass().isAssignableFrom(type)) {
				components.add((C) component);
			}
		}
		return components;
	}

	public List<ComponentService> getAll(ComponentType... types) {
		List<ComponentService> components = new ArrayList<ComponentService>();
		for (ComponentService component : values()) {
			for (ComponentType type : types) {
				if (component.getType() == type) {
					components.add(component);
				}
			}
		}
		return components;
	}

	public <C extends ComponentService> C get(Class<C> type) {
		List<C> components = getAll(type);
		if (components.size() > 0) {
			if (components.size() > 1) {
				logger.warn("Several components available for type: {}", type.getSimpleName());
			}
			return components.get(0);
		}
		return null;
	}

	public ComponentService get(ComponentType type) {
		List<ComponentService> components = getAll(type);
		if (components.size() > 0) {
			if (components.size() > 1) {
				logger.warn("Several components available for type: {}", type.getFullName());
			}
			return components.get(0);
		}
		return null;
	}

	public boolean containsType(ComponentType type) {
		List<ComponentService> components = getAll(type);
		return components.size() > 0;
	}

	public <C extends ComponentService> boolean contains(Class<C> type) {
		List<C> components = getAll(type);
		return components.size() > 0;
	}

}
