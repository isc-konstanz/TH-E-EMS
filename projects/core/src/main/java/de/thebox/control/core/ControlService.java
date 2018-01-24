package de.thebox.control.core;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public interface ControlService {

	public Preferences readComponentConfigs(String component) throws IOException;

	public boolean writeValue(String id, Value value);

	public void setLatestValue(String id, Value value);

	public Value getLatestValue(String id);

	public Value getLatestValue(String id, ValueListener listener);

	public void registerValueListener(String id, ValueListener listener);

	public void deregisterValueListener(String id, ValueListener listener);

}
