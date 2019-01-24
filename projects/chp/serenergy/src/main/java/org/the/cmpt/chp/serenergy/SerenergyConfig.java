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
package org.the.cmpt.chp.serenergy;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class SerenergyConfig extends Configurations {

	protected final static String TEMPERATURE_STACK_MAX_KEY = "stack_temp_max";
	protected final static String TEMPERATURE_STACK_MIN_KEY = "stack_temp_min";
	protected final static String TEMPERATURE_STACK_KEY = "stack_temp";

	public double getStackTemperatureMax() throws ConfigurationException {
		return getDouble(TEMPERATURE_STACK_MAX_KEY);
	}

	public double getStackTemperatureMin() throws ConfigurationException {
		return getDouble(TEMPERATURE_STACK_MIN_KEY);
	}

	public String getStackTemperature() throws ConfigurationException {
		return get(TEMPERATURE_STACK_KEY);
	}

}
