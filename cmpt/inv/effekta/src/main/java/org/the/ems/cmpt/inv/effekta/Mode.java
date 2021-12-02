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

public enum Mode {
//    DISABLED; 1010 1001 0000 0000 (43264d)49920
//    DEFAULT,
//    CHARGE_FROM_GRID, 1100 0001 0000 0000 (49408d)
//    FEED_INTO_GRID, 0010 1011 0000 0000 (11008)
	
    DISABLED("0010000100000000"),
    DEFAULT("1011011000000000"),
    CHARGE_FROM_GRID("1111100100000000"),
    FEED_INTO_GRID("0111111000000000");

    private final int mode;

    private Mode(String bitStr) {
    	this(Integer.parseUnsignedInt(bitStr, 2));
    }

    private Mode(int bits) {
        this.mode = bits;
    }

    public int getInt() {
        return mode;
    } 
    
    public String getByteArray() {
        return Integer.toBinaryString(mode);
    } 
	
}
