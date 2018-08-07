package de.thebox.control.core.data;

import java.util.Objects;

public class DoubleValue extends Value {

	private final double value;

	public DoubleValue(double value, long timestamp) {
		super(ValueType.DOUBLE, timestamp);
		this.value = value;
	}

	public DoubleValue(double value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return (float) value;
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
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof DoubleValue)) {
			return false;
		}
		DoubleValue user = (DoubleValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static DoubleValue emptyValue() {
		return new DoubleValue(0);
	}

}
