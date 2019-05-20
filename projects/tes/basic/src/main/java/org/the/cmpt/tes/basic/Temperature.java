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
package org.the.cmpt.tes.basic;

public enum Temperature {
	H10(10),
	H20(20),
	H30(30),
	H40(40),
	H50(50),
	H60(60),
	H70(70),
	H80(80),
	H90(90),
	H100(100);

	private final int height;

	public static final String[] KEYS = {"", ""};

	private Temperature(int height) {
		this.height = height;
	}

	@Override
	public String toString() {
		return "temp_h"+height;
	}

	public static String[] getKeys() {
		return new String[] {};
	}

}
