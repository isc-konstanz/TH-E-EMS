package de.thebox.control.core.data;

public abstract class Value {

	protected final ValueType type;
	protected final Long timestamp;

	public Value(ValueType type, Long timestamp) {
		this.timestamp = timestamp;
		this.type = type;
	}

	public ValueType getType() {
		return type;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public abstract double doubleValue();

	public abstract float floatValue();

	public abstract long longValue();

	public abstract int intValue();

	public abstract short shortValue();

	public abstract byte byteValue();

	public abstract boolean booleanValue();

}
