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
package org.the.ems.cmpt.vnt;

import java.util.LinkedList;
import java.util.List;

import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class TemperatureListener implements ValueListener {

	private final static int MOVING_AVG_SIZE = 6;

	/**
	 * Interface used to notify the {@link CabinetVentilation} 
	 * implementation about changed temperatures
	 */
	public interface TemperatureCallbacks {
	
		public void onTemperatureReceived(Temperature type, Double temperature);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed temperatures
	 */
	private final TemperatureCallbacks callbacks;

	private final Temperature type;

	private List<Double> temperatures = new LinkedList<Double>();
	private double temperatureSum = -1;

	public TemperatureListener(TemperatureCallbacks callbacks, Temperature type) {
		this.callbacks = callbacks;
		this.type = type;
	}

	public Temperature getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		Double temperature = value.doubleValue();
		
		if (temperatureSum == -1) {
			temperatureSum = temperature;
			temperatures.add(temperature);
		}
		else {
			while (temperatures.size() >= MOVING_AVG_SIZE) {
				temperatureSum -= temperatures.remove(0);
			}
			temperatures.add(temperature);
			temperatureSum += temperature;
		}
		
		Double average = temperatureSum/temperatures.size();
		callbacks.onTemperatureReceived(type, average);
	}
}
