/*
 * Copyright 2011-18 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
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
