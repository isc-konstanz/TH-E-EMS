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
package org.the.ems.core.config;

import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;

public class TestChannel implements Channel {

	private final String id;

	public TestChannel(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public void registerValueListener(ValueListener listener) {
	}

	@Override
	public void deregisterValueListener(ValueListener listener) {
	}

	@Override
	public Value getLatestValue(ValueListener listener) {
		return null;
	}

	@Override
	public Value getLatestValue() {
		return null;
	}

	@Override
	public void setLatestValue(Value value) {
	}

	@Override
	public void write(Value value) {
	}

	@Override
	public void write(ValueList value) {
	}

}
