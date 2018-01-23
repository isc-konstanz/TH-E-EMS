package de.thebox.control.core.data;

public class ShortValue extends Value {

	private final short value;

	public ShortValue(short value, Long timestamp) {
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

}
