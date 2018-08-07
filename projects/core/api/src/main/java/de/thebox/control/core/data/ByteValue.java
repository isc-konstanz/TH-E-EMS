package de.thebox.control.core.data;

import java.util.Objects;

public class ByteValue extends Value {

	private final byte value;

	public ByteValue(byte value, long timestamp) {
		super(ValueType.BYTE, timestamp);
		this.value = value;
	}

	public ByteValue(byte value) {
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
		return value;
	}

	@Override
	public boolean booleanValue() {
		return (value != 0);
	}

	@Override
	public String toString() {
		return Byte.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ByteValue)) {
			return false;
		}
		ByteValue user = (ByteValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static ByteValue emptyValue() {
		return new ByteValue((byte) 0);
	}

}
