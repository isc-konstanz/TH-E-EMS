package de.thebox.control.core.data;

import java.util.Objects;

public class BooleanValue extends Value {

	private final boolean value;

	public BooleanValue(boolean value, long timestamp) {
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

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof BooleanValue)) {
			return false;
		}
		BooleanValue user = (BooleanValue) o;
		return Objects.equals(time, user.time) &&
				Objects.equals(value, user.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, time, value);
	}

	public static BooleanValue emptyValue() {
		return new BooleanValue(false);
	}

}
