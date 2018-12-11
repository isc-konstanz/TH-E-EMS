/* 
 * Copyright 2016-18 ISC Konstanz
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
package de.thebox.control.core;

import java.io.IOException;
import java.util.prefs.Preferences;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueList;
import de.thebox.control.core.data.ValueListener;

public interface ControlService {

	public Preferences readComponentConfigs(String component) throws IOException;

	public Channel getChannel(String id) throws UnknownChannelException;

	public void registerValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public void deregisterValueListener(String id, ValueListener listener) throws UnknownChannelException;

	public Value getLatestValue(String id, ValueListener listener) throws UnknownChannelException;

	public Value getLatestValue(String id) throws UnknownChannelException;

	public void setLatestValue(String id, Value value) throws UnknownChannelException;

	public void write(String id, Value value) throws UnknownChannelException;

	public void write(String id, ValueList values) throws UnknownChannelException;

}
