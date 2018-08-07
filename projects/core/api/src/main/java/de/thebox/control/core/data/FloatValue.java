package de.thebox.control.core.data;

import java.util.Objects;

public class FloatValue extends Value {

	private final float value;

	public FloatValue(float value, long timestamp) {
		super(ValueType.FLOAT, timestamp);
		this.value = value;
	}

	public FloatValue(float value) {
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
		return (long) value;
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
		return (value != 0.0);
	}

	@Override
	public String toString() {
		return Float.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof FloatValue)) {
			return false;
		}
		FloatValue user = (FloatValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static FloatValue emptyValue() {
		return new FloatValue(0);
	}

}
