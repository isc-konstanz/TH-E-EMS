package de.thebox.control.core.schedule;

import java.util.Comparator;
import java.util.LinkedList;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public class Schedule extends LinkedList<Value> {
	private static final long serialVersionUID = -96966430241693416L;

	private final Integer interval;

	public Schedule(int interval) {
		this.interval = interval;
	}

	public Schedule() {
		this.interval = null;
	}

	public Integer getInterval() {
		return interval;
	}

	public long getStartTime() {
		Value start = peekFirst();
		if (start != null) {
			return start.getTimestamp();
		}
		return -1;
	}

	public long getEndTime() {
		Value end = peekLast();
		if (end != null) {
			if (interval != null) {
				return end.getTimestamp() + interval;
			}
			else if (size() > 1) {
				return end.getTimestamp() + (end.getTimestamp() - getStartTime())/size();
			}
		}
		return -1;
	}

	@Override
	public boolean add(Value value) {
		if (value.getTimestamp() <= getStartTime() || (interval != null && 
				(value.getTimestamp() >= getEndTime() || (value.getTimestamp() - getStartTime()) % interval != 0))) {
			return false;
		}
		return super.add(value);
	}

	public boolean add(long timestamp, double value) {
		return add(new DoubleValue(value, timestamp));
	}

	public boolean add(double value) {
		return add(new DoubleValue(value, getEndTime()));
	}

	public void sort() {
    	Comparator<Value> comparator = new SortTime();
		sort(comparator);
	}

	private class SortTime implements Comparator<Value> {
		@Override
		public int compare(Value v1, Value v2) {
			return (int) (v1.getTimestamp() - v2.getTimestamp());
		}
    }
}
