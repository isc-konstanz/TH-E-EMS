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
    DISABLED("0000000000000000"),
    DEFAULT("1011011000000000"),
    CHARGE_FROM_GRID("1110001000000000"),
    FEED_INTO_GRID("1011111000000000");

    private final short mode;

    private Mode(String bitStr) {
    	this((short) Integer.parseUnsignedInt(bitStr, 2));
    }

    private Mode(short bits) {
        this.mode = bits;
    }

    public short getShort() {
        return mode;
    } 
    
    public long getLong() {
        return (long)mode;
    } 
    
    public String getBinary() {
    	return Integer.toBinaryString(mode);
    }
    
    public String getByteArray() {
        return Integer.toBinaryString(mode);
    } 
	
}
