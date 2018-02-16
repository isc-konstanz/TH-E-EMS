package de.thebox.control.core.component;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;

public abstract class ComponentImpl implements ComponentService {
	protected ControlService control;

	public void bind(ControlService context) throws ComponentException {
		this.control = context;
		activate(load());
	}

	protected Preferences load() throws ComponentConfigException {
		try {
			return control.readComponentConfigs(getId());
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			throw new ComponentConfigException("Error while reading component configuration: " + e.getMessage());
		}
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		activate(load());
	}

}
