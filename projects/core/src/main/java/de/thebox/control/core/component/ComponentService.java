package de.thebox.control.core.component;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;

public interface ComponentService {

	public String getId();

	public default Component getType() {
		return Component.NONE;
	};

	public void bind(ControlService control) throws ComponentException;

	public void activate(Preferences config) throws ComponentException;

	public void reload() throws ComponentException;

	public void deactivate();

}
