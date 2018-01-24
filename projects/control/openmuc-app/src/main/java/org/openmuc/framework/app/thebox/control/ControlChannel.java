package org.openmuc.framework.app.thebox.control;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.RecordListener;

import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.ByteValue;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.FloatValue;
import de.thebox.control.core.data.IntValue;
import de.thebox.control.core.data.LongValue;
import de.thebox.control.core.data.ShortValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public class ControlChannel implements RecordListener {

	private final Channel channel;
	private final List<ValueListener> listeners;

	public ControlChannel(Channel channel) {
		this.channel = channel;
		this.listeners = new ArrayList<ValueListener>();
	}

	public boolean write(Value value) {
		Flag result = channel.write(ControlChannel.encodeValue(value));
		if (result == Flag.VALID) {
			return true;
		}
		return false;
	}

	public void setLatestValue(Value value) {
		channel.setLatestRecord(ControlChannel.encodeRecord(value));
	}

	public Value getLatestValue() {
		Record record = channel.getLatestRecord();
		return ControlChannel.decodeRecord(record, channel.getValueType());
	}

	public void register(ValueListener listener) {
		synchronized (listeners) {
			if (listeners.size() == 0) {
				channel.addListener(this);
			}
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	public void deregister(ValueListener listener) {
		synchronized (listeners) {
			if (listeners.contains(listener)) {
				listeners.remove(listener);
			}
			if (listeners.size() == 0) {
				channel.removeListener(this);
			}
		}
	}

	@Override
	public void newRecord(Record record) {
		Value value = ControlChannel.decodeRecord(record, channel.getValueType());
		if (value != null) {
			for (ValueListener listener : listeners) {
				listener.onValueReceived(value);
			}
		}
		// TODO: implement error warnings for certain flags
	}

	public static Value decodeRecord(org.openmuc.framework.data.Record record, org.openmuc.framework.data.ValueType type) {
		if (record.getFlag() == Flag.VALID) {
			Long time = record.getTimestamp();
			
			org.openmuc.framework.data.Value value = record.getValue();
			if (value != null) {
				try {
					switch(type) {
					case BOOLEAN:
						return new BooleanValue(value.asBoolean(), time);
					case BYTE:
						return new ByteValue(value.asByte(), time);
					case SHORT:
						return new ShortValue(value.asShort(), time);
					case INTEGER:
						return new IntValue(value.asInt(), time);
					case LONG:
						return new LongValue(value.asLong(), time);
					case FLOAT:
						return new FloatValue(value.asFloat(), time);
					default:
						return new DoubleValue(value.asDouble(), time);
					}
				}
				catch (ClassCastException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static org.openmuc.framework.data.Record encodeRecord(Value value) {
		org.openmuc.framework.data.Value recordValue = ControlChannel.encodeValue(value);
		return new org.openmuc.framework.data.Record(recordValue, value.getTimestamp(), Flag.VALID);
	}

	public static org.openmuc.framework.data.Value encodeValue(Value value) {
		if (value != null) {
			try {
				switch(value.getType()) {
				case BOOLEAN:
					return new org.openmuc.framework.data.BooleanValue(value.booleanValue());
				case BYTE:
					return new org.openmuc.framework.data.ByteValue(value.byteValue());
				case SHORT:
					return new org.openmuc.framework.data.ShortValue(value.shortValue());
				case INTEGER:
					return new org.openmuc.framework.data.IntValue(value.intValue());
				case LONG:
					return new org.openmuc.framework.data.LongValue(value.longValue());
				case FLOAT:
					return new org.openmuc.framework.data.FloatValue(value.floatValue());
				default:
					return new org.openmuc.framework.data.DoubleValue(value.doubleValue());
				}
			}
			catch (ClassCastException e) {
				return null;
			}
		}
		return null;
	}
}
