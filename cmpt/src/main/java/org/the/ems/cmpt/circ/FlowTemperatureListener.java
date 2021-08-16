/* 
 * Copyright 2016-2021 ISC Konstanz
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
package org.the.ems.cmpt.circ;

import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class FlowTemperatureListener implements ValueListener {

	/**
	 * Interface used to notify the {@link Circulation} 
	 * implementation about changed temperatures
	 */
	public interface CirculationTemperatureCallbacks {
		public void onTemperatureReceived(FlowTemperature type, Value temperature);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed temperatures
	 */
	private final CirculationTemperatureCallbacks callbacks;

	private final FlowTemperature type;

	public FlowTemperatureListener(CirculationTemperatureCallbacks callbacks, FlowTemperature type) {
		this.callbacks = callbacks;
		this.type = type;
	}

	public FlowTemperature getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		callbacks.onTemperatureReceived(type, value);
	}

}
