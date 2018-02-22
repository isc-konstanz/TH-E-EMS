package de.thebox.control.core.component;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;

public interface ComponentService {

	public String getId();

	public default Component getType() {
		return Component.NONE;
	};

	public void bind(ControlService control) throws ControlException;

	public void activate(Preferences config) throws ControlException;

	public void reload() throws ControlException;

	public void deactivate();

}
