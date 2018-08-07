package de.thebox.control.core.schedule;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueList;

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
