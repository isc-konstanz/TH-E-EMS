package de.thebox.control.core.schedule;

import java.util.ArrayList;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public class Schedule extends ArrayList<Value> {
	private static final long serialVersionUID = -96966430241693416L;

	private final Value start;
	private final long end;

	public Schedule(Value start, long end) {
		this.start = start;
		this.end = end;
	}

	public long getStartTime() {
		return start.getTimestamp();
	}

	public long getEndTime() {
		return end;
	}

	@Override
	public boolean add(Value value) {
		if (value.getTimestamp() > start.getTimestamp() &&
				value.getTimestamp() < end) {
			
			return super.add(value);
		}
		return false;
	}

	public boolean add(long timestamp, double value) {
		return add(new DoubleValue(value, timestamp));
	}
}
