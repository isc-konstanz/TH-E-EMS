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
package org.the.ems.core.data;

import java.util.LinkedHashMap;

public class WriteContainer extends LinkedHashMap<Channel, ValueList> {
	private static final long serialVersionUID = 7352264303947891380L;

	public WriteContainer() {
		super();
	}

	public boolean add(Channel channel, Value value) {
		if (value == null || channel == null) {
			throw new NullPointerException();
		}
		if (!containsKey(channel)) {
			put(channel, new ValueList());
		}
		return get(channel).add(value);
	}

	public boolean addDouble(Channel channel, double value, long time) {
		return add(channel, new DoubleValue(value, time));
	}

	public boolean addDouble(Channel channel, double value) {
		return add(channel, new DoubleValue(value));
	}

	public boolean addDoubleIfChanged(Channel channel, double value) throws InvalidValueException {
		if (channel.getLatestValue().doubleValue() == value) {
			return true;
		}
		return add(channel, new DoubleValue(value));
	}

	public boolean addFloat(Channel channel, float value, long time) {
		return add(channel, new FloatValue(value, time));
	}

	public boolean addFloat(Channel channel, float value) {
		return add(channel, new FloatValue(value));
	}

	public boolean addFloatIfChanged(Channel channel, float value) throws InvalidValueException {
		if (channel.getLatestValue().floatValue() == value) {
			return true;
		}
		return add(channel, new FloatValue(value));
	}

	public boolean addLong(Channel channel, long value, long time) {
		return add(channel, new LongValue(value, time));
	}

	public boolean addLong(Channel channel, long value) {
		return add(channel, new LongValue(value));
	}

	public boolean addLongIfChanged(Channel channel, long value) throws InvalidValueException {
		if (channel.getLatestValue().longValue() == value) {
			return true;
		}
		return add(channel, new LongValue(value));
	}

	public boolean addInteger(Channel channel, int value, long time) {
		return add(channel, new IntValue(value, time));
	}

	public boolean addInteger(Channel channel, int value) {
		return add(channel, new IntValue(value));
	}

	public boolean addIntegerIfChanged(Channel channel, int value) throws InvalidValueException {
		if (channel.getLatestValue().intValue() == value) {
			return true;
		}
		return add(channel, new IntValue(value));
	}

	public boolean addShort(Channel channel, short value, long time) {
		return add(channel, new ShortValue(value, time));
	}

	public boolean addShort(Channel channel, short value) {
		return add(channel, new ShortValue(value));
	}

	public boolean addShortIfChanged(Channel channel, short value) throws InvalidValueException {
		if (channel.getLatestValue().shortValue() == value) {
			return true;
		}
		return add(channel, new ShortValue(value));
	}

	public boolean addByte(Channel channel, byte value, long time) {
		return add(channel, new ByteValue(value, time));
	}

	public boolean addByte(Channel channel, byte value) {
		return add(channel, new ByteValue(value));
	}

	public boolean addByteIfChanged(Channel channel, byte value) throws InvalidValueException {
		if (channel.getLatestValue().byteValue() == value) {
			return true;
		}
		return add(channel, new ByteValue(value));
	}

	public boolean addBoolean(Channel channel, boolean value, long time) {
		return add(channel, new BooleanValue(value, time));
	}

	public boolean addBoolean(Channel channel, boolean value) {
		return add(channel, new BooleanValue(value));
	}

	public boolean addBooleanIfChanged(Channel channel, boolean value) throws InvalidValueException {
		if (channel.getLatestValue().booleanValue() == value) {
			return true;
		}
		return add(channel, new BooleanValue(value));
	}

	public void add(WriteContainer channels) {
		if (channels == null) {
			return;
		}
		for (Channel channel : channels.keySet()) {
			for (Value value : channels.get(channel)) {
				add(channel, value);
			}
		}
	}

}
