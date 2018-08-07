package de.thebox.control.core.data;

public abstract class Value {

	protected final ValueType type;
	protected final long time;

	public Value(ValueType type, long timestamp) {
		this.time = timestamp;
		this.type = type;
	}

	public ValueType getType() {
		return type;
	}

	public long getTime() {
		return time;
	}

	public abstract double doubleValue();

	public abstract float floatValue();

	public abstract long longValue();

	public abstract int intValue();

	public abstract short shortValue();

	public abstract byte byteValue();

	public abstract boolean booleanValue();

}
