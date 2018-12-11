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
package de.thebox.control.core.component.circ;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.Value;

public class CirculationTemperatureListener extends ChannelListener {

	/**
	 * Interface used to notify the {@link Circulation} 
	 * implementation about changed temperatures
	 */
	public interface CirculationTemperatureCallbacks {
		public void onTemperatureReceived(CirculationTemperature type, Value temperature);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed temperatures
	 */
	private final CirculationTemperatureCallbacks callbacks;

	private final CirculationTemperature type;

	public CirculationTemperatureListener(CirculationTemperatureCallbacks callbacks, CirculationTemperature type, Channel channel) {
		super(channel);
		
		this.callbacks = callbacks;
		this.type = type;
	}

	public CirculationTemperature getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		callbacks.onTemperatureReceived(type, value);
	}
}
