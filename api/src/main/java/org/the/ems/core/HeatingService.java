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
package org.the.ems.core;

import org.the.ems.core.data.Value;

public interface HeatingService extends ComponentService {

	public GeneratorState getState();

	/*
	 * Get the current runtime in milliseconds [ms].
	 */
	public int getRuntime();

	/*
	 * Get the minimum runtime in milliseconds [ms].
	 */
	public int getMinRuntime();

	/*
	 * Get the maximum power in watts [W].
	 */
	public double getMaxPower();

	/*
	 * Get the minimum power in watts [W].
	 */
	public double getMinPower();

	/*
	 * Get the generated thermal energy in kilowatt hours [kWh].
	 */
	public Value getThermalEnergy() throws ComponentException;

	/*
	 * Get the generated thermal power in watts [W].
	 */
	public Value getThermalPower() throws ComponentException;

	/*
	 * Start the generation.
	 */
	public void start(Value value) throws EnergyManagementException;

	/*
	 * Stop the generation.
	 */
	public void stop(long time) throws EnergyManagementException;

	public default void stop() throws EnergyManagementException {
		stop(System.currentTimeMillis());
	}

}
