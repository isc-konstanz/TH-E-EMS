package de.thebox.control.core.data;

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

}
