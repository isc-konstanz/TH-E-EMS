package de.thebox.control.core.data;

public class ByteValue extends Value {

	private final byte value;

	public ByteValue(byte value, Long timestamp) {
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

}
