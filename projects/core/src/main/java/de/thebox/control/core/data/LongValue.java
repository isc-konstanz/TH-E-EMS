package de.thebox.control.core.data;

import java.util.Objects;

public class LongValue extends Value {

	private final long value;

	public LongValue(long value, Long timestamp) {
		super(ValueType.LONG, timestamp);
		this.value = value;
	}

	public LongValue(long value) {
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
		return (int) value;
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
		return Long.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof LongValue)) {
			return false;
		}
		LongValue user = (LongValue) o;
		return Objects.equals(timestamp, user.timestamp) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, timestamp, value);
	}

	public static LongValue emptyValue() {
		return new LongValue(0);
	}

}
