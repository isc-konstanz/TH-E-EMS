package de.thebox.control.core;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.core.schedule.Schedule;

public interface ControlService {

	public Preferences readComponentConfigs(String component) throws IOException;

	public Channel getChannel(String id) throws UnknownChannelException;

	public void registerValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public void deregisterValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public Value getLatestValue(String id, ValueListener listener) throws UnknownChannelException;

	public Value getLatestValue(String id) throws UnknownChannelException;

	public void setLatestValue(String id, Value value) throws UnknownChannelException;

	public void write(String id, Value value) throws UnknownChannelException;

	public void schedule(String id, Schedule schedule) throws UnknownChannelException;

}
