package org.openmuc.framework.app.thebox.control;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.RecordListener;

import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.ByteValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.FloatValue;
import de.thebox.control.core.data.IntValue;
import de.thebox.control.core.data.LongValue;
import de.thebox.control.core.data.ShortValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueList;
import de.thebox.control.core.data.ValueListener;

public class ControlChannel implements Channel, RecordListener {

	/**
	 * Interface used to notify the {@link Control} 
	 * implementation about changed temperatures
	 */
	public interface ControlChannelCallbacks {
		public void execute(Runnable task);
	}

	/**
	 * The Channels' current callback object, which is notified of changed temperatures
	 */
	private final ControlChannelCallbacks callbacks;

	private final org.openmuc.framework.dataaccess.Channel channel;
	private final List<ValueListener> listeners;

	public ControlChannel(ControlChannelCallbacks callbacks, org.openmuc.framework.dataaccess.Channel channel) {
		this.callbacks = callbacks;
		this.channel = channel;
		this.listeners = new ArrayList<ValueListener>();
	}

	@Override
	public void registerValueListener(ValueListener listener) {
		synchronized (listeners) {
			if (listeners.size() == 0) {
				channel.addListener(this);
			}
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	@Override
	public void deregisterValueListener(ValueListener listener) {
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
	public Value getLatestValue(ValueListener listener) {
		registerValueListener(listener);
		return getLatestValue();
	}

	@Override
	public Value getLatestValue() {
		Record record = channel.getLatestRecord();
		return ControlChannel.decodeRecord(record, channel.getValueType());
	}

	@Override
	public void setLatestValue(Value value) {
		Runnable task = () -> {
			channel.setLatestRecord(ControlChannel.encodeRecord(value));
		};
		callbacks.execute(task);
	}

	@Override
	public void write(Value value) {
		Runnable task = () -> {
			if (value.getTime() <= System.currentTimeMillis()) {
				channel.write(ControlChannel.encodeValue(value));
			}
			else {
				channel.writeFuture(ControlChannel.encodeFutureValueList(new ValueList(value)));
			}
		};
		callbacks.execute(task);
	}

	@Override
	public void write(ValueList values) {
		Runnable task = () -> {
			long time = System.currentTimeMillis();
			
			ListIterator<Value> iter = values.sort().listIterator();
			while(iter.hasNext()) {
				Value value = iter.next();
				if(value.getTime() <= time) {
					iter.remove();
					
					channel.write(ControlChannel.encodeValue(value));
				}
			}
			channel.writeFuture(ControlChannel.encodeFutureValueList(values));
		};
		callbacks.execute(task);
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
				}
			}
		}
		return null;
	}

	public static org.openmuc.framework.data.Record encodeRecord(Value value) {
		org.openmuc.framework.data.Value recordValue = ControlChannel.encodeValue(value);
		return new org.openmuc.framework.data.Record(recordValue, value.getTime(), Flag.VALID);
	}

	public static List<org.openmuc.framework.data.FutureValue> encodeFutureValueList(ValueList values) {
		List<org.openmuc.framework.data.FutureValue> futures = new LinkedList<>();
		for (Value value : values) {
			org.openmuc.framework.data.FutureValue future = encodeFutureValue(value);
			if (future != null) {
				futures.add(future);
			}
		}
		return futures;
	}

	public static org.openmuc.framework.data.FutureValue encodeFutureValue(Value value) {
		org.openmuc.framework.data.Value future = encodeValue(value);
		if (future != null) {
			return new org.openmuc.framework.data.FutureValue(future, value.getTime());
		}
		return null;
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

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ControlChannel)) {
			return false;
		}
		ControlChannel user = (ControlChannel) o;
		return Objects.equals(channel.getId(), user.channel.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(channel.getId());
	}

}