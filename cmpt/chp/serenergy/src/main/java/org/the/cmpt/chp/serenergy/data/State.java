/* 
 * Copyright 2016-20 ISC Konstanz
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
package org.the.cmpt.chp.serenergy.data;

import java.util.HashMap;
import java.util.Map;

import org.the.ems.core.data.IntValue;
import org.the.ems.core.data.Value;

public enum State {
	UNKNOWN(0),
	PASSIVE(1),
	STANDBY(2),
	PREHEAT(3),
	STARTING(4),
	OPERATION_LIMITED(5),
	OPERATION(6),
	SHUTDOWN(7),
	SHUTDOWN_EMERGENCY(8),
	ERROR(9),
	ZERO_LOAD_MODE(10);

	private static final Map<Integer, State> codes = new HashMap<>();

	private final int code;

	private State(int code) {
		this.code = code;
	}

	public Value encode() {
		return new IntValue(code);
	}

	public Value encode(long timestamp) {
		return new IntValue(code, timestamp);
	}

	public static State decode(Value value) {
		if (value == null) {
			return UNKNOWN;
		}
		State enumInstance = codes.get(value.intValue());
		if (enumInstance == null) {
			throw new IllegalArgumentException("Unknown state code: " + value.intValue());
		}
		return enumInstance;
	}

	static {
		for (State s : State.values()) {
			if (codes.put(s.code, s) != null) {
				throw new IllegalArgumentException("Duplicate code: " + s.code);
			}
		}
	}
}
