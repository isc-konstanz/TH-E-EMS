package de.thebox.control.core.data;

import java.util.Objects;

public class IntValue extends Value {

	private final int value;

	public IntValue(int value, long timestamp) {
		super(ValueType.INTEGER, timestamp);
		this.value = value;
	}

	public IntValue(int value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public short shortValue() {
		return (short) value;
	}

	@Override
	public byte byteValue() {
		return (byte) value;
	}

	@Override
	public boolean booleanValue() {
		return (value != 0);
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof IntValue)) {
			return false;
		}
		IntValue user = (IntValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static IntValue emptyValue() {
		return new IntValue(0);
	}

}
