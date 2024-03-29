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
package org.the.ems.core.data;

import java.util.LinkedList;
import java.util.List;

import org.the.ems.core.EnergyManagementException;

public class ChannelListener implements Channel {

    private final List<ValueListener> listeners = new LinkedList<ValueListener>();

    private final Channel channel;

    public ChannelListener(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String getId() {
        return channel.getId();
    }

    @Override
    public Value getLatestValue() throws InvalidValueException {
        return channel.getLatestValue();
    }

    @Override
    public Value getLatestValue(ValueListener listener) throws InvalidValueException {
        this.registerValueListener(listener);
        return channel.getLatestValue(listener);
    }

    @Override
    public void registerValueListener(ValueListener listener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                channel.registerValueListener(listener);
            }
        }
    }

    @Override
    public void deregisterValueListener(ValueListener listener) {
        synchronized(listeners) {
            if (listeners.contains(listener)) {
                listeners.remove(listener);
                channel.deregisterValueListener(listener);
            }
        }
    }

	@Override
	public void deregisterValueListeners() {
        synchronized(listeners) {
            for (ValueListener listener : listeners) {
                channel.deregisterValueListener(listener);
            }
            listeners.clear();
        }
    }

    @Override
    public void setLatestValue(Value value) {
        channel.setLatestValue(value);
    }

    @Override
    public void write(Value value) throws EnergyManagementException {
        channel.write(value);
    }

    @Override
    public void write(ValueList value) throws EnergyManagementException {
        channel.write(value);
    }

}
