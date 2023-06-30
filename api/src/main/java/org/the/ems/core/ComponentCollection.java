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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentCollection extends HashMap<String, Component> {
	private static final long serialVersionUID = -2207305169561493106L;

	private final static Logger logger = LoggerFactory.getLogger(ComponentCollection.class);

	public Component add(Component component) {
		return put(component.getId(), component);
	}

	public <C extends Component> void addAll(List<C> components) {
		for (C component : components) {
			put(component.getId(), component);
		}
	}

	public <C extends Component> Deque<Component> getAll(Class<C> type) {
		Deque<Component> components = new ArrayDeque<Component>();
		for (Component component : values()) {
			if (type.isAssignableFrom(component.getClass())) {
				components.add(component);
			}
		}
		return components;
	}

	public Deque<Component> getAll(ComponentType... types) {
		Deque<Component> components = new ArrayDeque<Component>();
		for (Component component : values()) {
			for (ComponentType type : types) {
				if (component.getType() == type) {
					components.add(component);
				}
			}
		}
		return components;
	}

	public <C extends Component> Component get(Class<C> type) {
		Deque<Component> components = getAll(type);
		if (components.size() > 0) {
			if (components.size() > 1) {
				logger.warn("Several components available for type: {}", type.getSimpleName());
			}
			return components.getFirst();
		}
		return null;
	}

	public Component get(ComponentType type) {
		Deque<Component> components = getAll(type);
		if (components.size() > 0) {
			if (components.size() > 1) {
				logger.warn("Several components available for type: {}", type.getFullName());
			}
			return components.getFirst();
		}
		return null;
	}

	public boolean containsType(ComponentType type) {
		Deque<Component> components = getAll(type);
		return components.size() > 0;
	}

	public <C extends Component> boolean contains(Class<C> type) {
		Deque<Component> components = getAll(type);
		return components.size() > 0;
	}

}
