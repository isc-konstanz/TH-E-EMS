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
package org.the.ems.cmpt.inv.effekta;

import org.the.ems.core.data.Value;

public enum Mode {
    DISABLED("00000000"),
    DEFAULT("10111000"),
    CHARGE_FROM_GRID("11011000"),
    FEED_INTO_GRID("10100110");

    private final int mode;

    private Mode(String bitStr) {
    	this(Integer.parseUnsignedInt(bitStr+"00000000", 2));
    }

    private Mode(int bits) {
        this.mode = bits;
    }

    public int getInteger() {
    	return mode;
    }

    public String toBinaryString() {
        return toBinaryString(mode);
    }

    public static String toBinaryString(int value) {
    	String valueStr = Integer.toBinaryString(value);
    	return "0".repeat(16 - valueStr.length()) + valueStr;
    }

    public boolean equals(Value value) {
    	return getInteger() == value.intValue();
    }

}
