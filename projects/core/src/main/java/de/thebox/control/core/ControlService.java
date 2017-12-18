package de.thebox.control.core;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.data.Value;

public interface ControlService {
	
	public Preferences readComponentConfigs(String component) throws IOException;
	
	public Value getLatestValue(String id);
	
	public Value getLatestValue(String id, ControlValueListener listener);
	
	public void registerValueListener(String id, ControlValueListener listener);
	
	public void deregisterValueListener(String id, ControlValueListener listener);
	
	public boolean setValue(String id, Value value);

}
