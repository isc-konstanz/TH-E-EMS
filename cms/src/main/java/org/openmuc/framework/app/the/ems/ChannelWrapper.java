/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmuc.framework.app.the.ems;

import static org.the.ems.core.data.InvalidValueException.Severity.ERROR;
import static org.the.ems.core.data.InvalidValueException.Severity.INFO;
import static org.the.ems.core.data.InvalidValueException.Severity.WARNING;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.RecordListener;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ByteValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.FloatValue;
import org.the.ems.core.data.IntValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.LongValue;
import org.the.ems.core.data.ShortValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.event.ErrorEvent;
import org.the.ems.core.data.event.Event;
import org.the.ems.core.data.event.EventType;
import org.the.ems.core.data.event.ValueEvent;

public class ChannelWrapper implements Channel, RecordListener {

	/**
	 * Interface used to notify the {@link ContentManager} 
	 * implementation about changed temperatures
	 */
	public interface ChannelCallbacks {
		public void doExecute(Runnable task);
	}

	/**
	 * The Channels' current callback object, which is notified of changed temperatures
	 */
	protected final ChannelCallbacks callbacks;

	protected final org.openmuc.framework.dataaccess.Channel channel;
	protected final List<ValueListener> listeners;

	protected Value lastValue = null;

	public ChannelWrapper(ChannelCallbacks callbacks, org.openmuc.framework.dataaccess.Channel channel) {
		this.callbacks = callbacks;
		this.channel = channel;
		this.listeners = new ArrayList<ValueListener>();
	}

	@Override
	public String getId() {
		return channel.getId();
	}

	@Override
	public Value getLatestValue() throws InvalidValueException {
		return decodeRecord(channel.getLatestRecord(), 
							channel.getValueType());
	}

	@Override
	public Value getLatestValue(ValueListener listener) throws InvalidValueException {
		registerValueListener(listener);
		return getLatestValue();
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
	public void setLatestValue(Value value) {
		Runnable task = () -> {
			channel.setLatestRecord(encodeRecord(value));
		};
		callbacks.doExecute(task);
	}

	@Override
	public void write(Value value) {
		Runnable task = () -> {
			if (value.getTime() <= System.currentTimeMillis()) {
				channel.write(encodeValue(value));
			}
			else {
				channel.writeFuture(encodeFutureValueList(new ValueList(value)));
			}
		};
		callbacks.doExecute(task);
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
					
					channel.write(encodeValue(value));
				}
			}
			channel.writeFuture(encodeFutureValueList(values));
		};
		callbacks.doExecute(task);
	}

	@Override
	public void newRecord(Record record) {
		try {
			Value newValue = decodeRecord(record, channel.getValueType());
			newValueEvent(newValue, EventType.RECEIVED);
			if (hasValueChanged(newValue)) {
				newValueEvent(newValue, EventType.CHANGED);
			}
			lastValue = newValue;
			
		} catch (InvalidValueException e) {
			switch (e.getSeverity()) {
			case ERROR:
				newErrorEvent(e.getMessage());
			default:
				break;
			}
		}
	}

	private void newValueEvent(Value value, EventType type) {
		newEvent(new ValueEvent(this, value, type));
	}

	private void newErrorEvent(String message) {
		newEvent(new ErrorEvent(this, message));
	}

	private void newEvent(Event event) {
		for (ValueListener valueListener : listeners) {
			valueListener.onEvent(event);
			
			switch (event.getType()) {
			case RECEIVED:
				valueListener.onValueReceived(((ValueEvent) event).getValue());
				break;
			case CHANGED:
				valueListener.onValueChanged(((ValueEvent) event).getValue());
				break;
			case ERROR:
				valueListener.onError(((ErrorEvent) event).getError());
				break;
			}
		}
	}

	private boolean hasValueChanged(Value newValue) {
		if (this.lastValue == null) {
			return true;
		}
		switch(channel.getValueType()) {
		case BOOLEAN:
			return newValue.booleanValue() != lastValue.booleanValue();
		case BYTE:
			return newValue.byteValue() != lastValue.byteValue();
		case SHORT:
			return newValue.shortValue() != lastValue.shortValue();
		case INTEGER:
			return newValue.intValue() != lastValue.intValue();
		case LONG:
			return newValue.longValue() != lastValue.longValue();
		case FLOAT:
			return newValue.floatValue() != lastValue.floatValue();
		default:
			return newValue.doubleValue() != lastValue.doubleValue();
		}
	}

	private Value decodeRecord(org.openmuc.framework.data.Record record, org.openmuc.framework.data.ValueType type) 
			throws InvalidValueException {
		
		if (record == null) {
			throw new InvalidValueException(this, ERROR, "Record is null");
		}
		if (Flag.VALID != record.getFlag()) {
			Flag flag = record.getFlag();
			String message = record.getFlag().toString();
			message = message.toLowerCase().replaceAll("_", " ");
			message = message.substring(0, 1).toUpperCase() + message.substring(1);
			
			switch(flag) {
			case VALID:
				break;
			case DISABLED:
			case DRIVER_UNAVAILABLE:
			case NO_VALUE_RECEIVED_YET:
			case SAMPLING_AND_LISTENING_DISABLED:
				throw new InvalidValueException(this, INFO, message);
			case CONNECTING:
			case DISCONNECTING:
			case WAITING_FOR_CONNECTION_RETRY:
				throw new InvalidValueException(this, WARNING, message);
			default:
				throw new InvalidValueException(this, ERROR, message);
			}
		}
		org.openmuc.framework.data.Value value = record.getValue();
		if (value == null) {
			throw new InvalidValueException(this, ERROR, "Value is null");
		}
		
		Long time = record.getTimestamp();
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
			throw new InvalidValueException(this, ERROR, e);
		}
	}

	private static org.openmuc.framework.data.Record encodeRecord(Value value) {
		org.openmuc.framework.data.Value recordValue = encodeValue(value);
		return new org.openmuc.framework.data.Record(recordValue, value.getTime(), Flag.VALID);
	}

	private static List<org.openmuc.framework.data.FutureValue> encodeFutureValueList(ValueList values) {
		List<org.openmuc.framework.data.FutureValue> futures = new LinkedList<>();
		for (Value value : values) {
			org.openmuc.framework.data.FutureValue future = encodeFutureValue(value);
			if (future != null) {
				futures.add(future);
			}
		}
		return futures;
	}

	private static org.openmuc.framework.data.FutureValue encodeFutureValue(Value value) {
		org.openmuc.framework.data.Value future = encodeValue(value);
		if (future != null) {
			return new org.openmuc.framework.data.FutureValue(future, value.getTime());
		}
		return null;
	}

	private static org.openmuc.framework.data.Value encodeValue(Value value) {
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
		if (!(o instanceof ChannelWrapper)) {
			return false;
		}
		ChannelWrapper user = (ChannelWrapper) o;
		return Objects.equals(channel.getId(), user.channel.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(channel.getId());
	}

}
