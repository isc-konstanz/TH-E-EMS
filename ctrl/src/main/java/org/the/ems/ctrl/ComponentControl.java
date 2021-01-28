package org.the.ems.ctrl;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;

abstract class ComponentControl<C extends ComponentService> {

	protected final ControlCallbacks callbacks;

	protected final C component;

	protected ComponentControl(ControlCallbacks callbacks, C component) throws ComponentException {
		this.component = component;
		this.callbacks = callbacks;
	}

	public String getId() {
		return component.getId();
	}

	public ComponentType getType() {
		return component.getType();
	}

	public C getComponent() {
		return component;
	}

}
