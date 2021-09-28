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
package org.the.ems.core.schedule;

import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;

public class Schedule extends ValueList {
	private static final long serialVersionUID = -96966430241693416L;

	protected final int interval;

	public Schedule(int interval, Value value) {
		this(interval);
		super.add(value);
	}

	public Schedule(int interval) {
		super();
		this.interval = interval;
	}

	public int getInterval() {
		return interval;
	}

	public long getStartTime() {
		Value start = peekFirst();
		if (start != null) {
			return start.getTime();
		}
		return -1;
	}

	public long getEndTime() {
		Value end = peekLast();
		if (end != null) {
			return end.getTime() + interval;
		}
		return -1;
	}

	@Override
	public boolean add(Value value) {
		if (value.getTime() <= getStartTime() || (value.getTime() - getStartTime()) % interval != 0 || 
				value.getTime() >= getEndTime()) {
			return false;
		}
		return super.add(value);
	}

	public boolean add(long time, double value) {
		return add(new DoubleValue(value, time));
	}

	public boolean add(double value) {
		return add(getEndTime(), value);
	}

}
