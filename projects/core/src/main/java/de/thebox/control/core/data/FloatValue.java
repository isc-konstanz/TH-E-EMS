package de.thebox.control.core.data;

public class FloatValue extends Value {
	
	private final float value;
	
	public FloatValue(float value, Long timestamp) {
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

}
