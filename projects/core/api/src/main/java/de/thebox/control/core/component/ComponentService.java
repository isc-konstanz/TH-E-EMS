package de.thebox.control.core.component;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface ComponentService {

	public String getId();

	public default Component getType() {
		return Component.NONE;
	};

	public ComponentStatus getStatus();

	public void setStatus(ComponentStatus status) throws ControlException;

	public void bind(ControlService control) throws ControlException;

	public void activate(Preferences config) throws ControlException;

	public void reload() throws ControlException;

	public void set(Value value) throws ControlException;

	public void schedule(Schedule schedule) throws ControlException;

	public void deactivate();

}
