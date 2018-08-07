package de.thebox.control.core.data;

import java.util.Objects;

public class ShortValue extends Value {

	private final short value;

	public ShortValue(short value, long timestamp) {
		super(ValueType.SHORT, timestamp);
		this.value = value;
	}

	public ShortValue(short value) {
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
		return value;
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
		return Short.toString(value);
	}

	@Override
	public boolean equals(Object o) {

		if (o == this) return true;
		if (!(o instanceof ShortValue)) {
			return false;
		}
		ShortValue user = (ShortValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static ShortValue emptyValue() {
		return new ShortValue((short) 0);
	}

}
