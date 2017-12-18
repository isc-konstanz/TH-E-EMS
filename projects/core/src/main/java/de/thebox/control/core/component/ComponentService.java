package de.thebox.control.core.component;

import de.thebox.control.core.ControlService;

public interface ComponentService {
	
	public String getId();
	
	public void activate(ControlService control) throws ComponentException;
	
	public void reload() throws ComponentException;
	
	public void deactivate();

}
