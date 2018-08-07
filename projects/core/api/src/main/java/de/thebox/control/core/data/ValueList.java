package de.thebox.control.core.data;

import java.util.Comparator;
import java.util.LinkedList;

public class ValueList extends LinkedList<Value> {
	private static final long serialVersionUID = -5725862833767844148L;

	public ValueList() {
		super();
	}

	public ValueList(Value value) {
		super();
		add(value);
	}

	public ValueList sort() {
    	Comparator<Value> comparator = new SortTime();
		sort(comparator);
		
		return this;
	}

	private class SortTime implements Comparator<Value> {
		@Override
		public int compare(Value v1, Value v2) {
			return (int) (v1.getTime() - v2.getTime());
		}
    }
}
