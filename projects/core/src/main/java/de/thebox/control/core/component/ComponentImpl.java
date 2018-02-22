package de.thebox.control.core.component;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.config.ConfigurationException;

public abstract class ComponentImpl implements ComponentService {
	protected ControlService control;

	public void bind(ControlService context) throws ControlException {
		this.control = context;
		activate(load());
	}

	protected Preferences load() throws ConfigurationException {
		try {
			return control.readComponentConfigs(getId());
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			throw new ConfigurationException("Error while reading component configuration: " + e.getMessage());
		}
	}

	@Override
	public void reload() throws ControlException {
		deactivate();
		activate(load());
	}

}
