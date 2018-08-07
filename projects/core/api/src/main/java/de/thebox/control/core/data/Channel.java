package de.thebox.control.core.data;

public interface Channel {

	public void registerValueListener(ValueListener listener);

	public void deregisterValueListener(ValueListener listener);

	public Value getLatestValue(ValueListener listener);

	public Value getLatestValue();

	public void setLatestValue(Value value);

	public void write(Value value);

	public void write(ValueList value);

}
