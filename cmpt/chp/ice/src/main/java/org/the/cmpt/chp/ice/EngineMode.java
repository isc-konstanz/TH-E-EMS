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
package org.the.cmpt.chp.ice;

import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Value;

public enum EngineMode {

	STAR(false),
	DELTA(true);

	private final boolean mode;

	private EngineMode(boolean mode) {
		this.mode = mode;
	}

	public boolean getMode() {
		return mode;
	}

	public Value getValue(long time) {
		return new BooleanValue(getMode(), time);
	}

	public Value getValue() {
		return new BooleanValue(getMode(), System.currentTimeMillis());
	}

	public static EngineMode valueOf(boolean mode) {
		if (mode) {
			return DELTA;
		}
		else {
			return STAR;
		}
	}
}
