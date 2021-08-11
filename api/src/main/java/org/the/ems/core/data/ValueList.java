/* 
 * Copyright 2016-21 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
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
