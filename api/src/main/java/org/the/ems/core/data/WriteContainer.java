/* 
 * Copyright 2016-19 ISC Konstanz
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

import java.util.LinkedHashMap;

public class WriteContainer extends LinkedHashMap<Channel, ValueList> {
	private static final long serialVersionUID = 7352264303947891380L;

	public WriteContainer() {
		super();
	}

	public boolean add(Channel channel, Value value) {
		if (!containsKey(channel)) {
			put(channel, new ValueList());
		}
		return get(channel).add(value);
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
