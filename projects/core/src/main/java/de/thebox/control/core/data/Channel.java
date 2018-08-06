package de.thebox.control.core.data;

import de.thebox.control.core.schedule.Schedule;

public interface Channel {

	public void registerValueListener(ValueListener listener);

	public void deregisterValueListener(ValueListener listener);

	public Value getLatestValue(ValueListener listener);

	public Value getLatestValue();

	public void setLatestValue(Value value);

	public void write(Value value);

	public void schedule(Schedule schedule);

}
