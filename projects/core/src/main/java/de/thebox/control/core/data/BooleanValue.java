package de.thebox.control.core.data;

public class BooleanValue extends Value {

	private final boolean value;

	public BooleanValue(boolean value, Long timestamp) {
		super(ValueType.BOOLEAN, timestamp);
		this.value = value;
	}

	public BooleanValue(boolean value) {
		this(value, System.currentTimeMillis());
	}

	@Override
	public double doubleValue() {
		if (value) {
			return 1.0;
		}
		else {
			return 0.0;
		}
	}

	@Override
	public float floatValue() {
		if (value) {
			return 1.0f;
		}
		else {
			return 0.0f;
		}
	}

	@Override
	public long longValue() {
		if (value) {
			return 1;
		}
		else {
			return 0;
		}
	}

	@Override
	public int intValue() {
		if (value) {
			return 1;
		}
		else {
			return 0;
		}
	}

	@Override
	public short shortValue() {
		if (value) {
			return 1;
		}
		else {
			return 0;
		}
	}

	@Override
	public byte byteValue() {
		if (value) {
			return 1;
		}
		else {
			return 0;
		}
	}

	@Override
	public boolean booleanValue() {
		return value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

}
