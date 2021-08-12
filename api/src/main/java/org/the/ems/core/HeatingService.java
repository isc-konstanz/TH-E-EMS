/* 
 * Copyright 2016-21 ISC Konstanz
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
package org.the.ems.core;

import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

public interface HeatingService extends RunnableService {

	/*
	 * Get the default value with which a component will be started with.
	 */
	@Override
	public default Value getStartValue(long time) {
		return new DoubleValue(getStartPower(), time);
	}

	/*
	 * Get the default power with which a component will be started with in watts [W].
	 */
	public double getStartPower();

	/*
	 * Get the minimum power in watts [W].
	 */
	public double getMinPower();

	/*
	 * Get the maximum power in watts [W].
	 */
	public double getMaxPower();

	/*
	 * Get the generated thermal energy in kilowatt hours [kWh].
	 */
	public Value getThermalEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated thermal power in watts [W].
	 */
	public Value getThermalPower() throws ComponentException, InvalidValueException;

}
