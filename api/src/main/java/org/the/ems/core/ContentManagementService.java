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
package org.the.ems.core;

import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;

public interface ContentManagementService {

	public Channel getChannel(String id) throws UnknownChannelException;

	public void registerValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public void deregisterValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public Value getLatestValue(String id, ValueListener listener) throws UnknownChannelException, InvalidValueException;

	public Value getLatestValue(String id) throws UnknownChannelException, InvalidValueException;

	public void setLatestValue(String id, Value value) throws UnknownChannelException;

	public void write(String id, Value value) throws UnknownChannelException, EnergyManagementException;

	public void write(String id, ValueList values) throws UnknownChannelException, EnergyManagementException;

}
