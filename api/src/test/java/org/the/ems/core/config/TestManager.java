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

import org.the.ems.core.ContentManagementService;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;

public class TestManager implements ContentManagementService {

	@Override
	public Channel getChannel(String id) throws UnknownChannelException {
		return new TestChannel(id);
	}

	@Override
	public void registerValueListener(String id, ValueListener listener) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public void deregisterValueListener(String id, ValueListener listener) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public Value getLatestValue(String id, ValueListener listener) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public Value getLatestValue(String id) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public void setLatestValue(String id, Value value) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public void write(String id, Value value) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

	@Override
	public void write(String id, ValueList values) throws UnknownChannelException {
		throw new UnknownChannelException();
	}

}
