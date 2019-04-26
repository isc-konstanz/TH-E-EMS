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

public interface CogeneratorService extends ComponentService {

	@Override
	public default ComponentType getType() {
		return ComponentType.COMBINED_HEAT_POWER;
	};

	/*
	 * Get the maximum electrical power in [W].
	 */
	public double getMaxPower();

	/*
	 * Get the minimum electrical power in [W].
	 */
	public double getMinPower();

	/*
	 * Get the electrical efficiency in [%] between 0 and 100%.
	 */
	public double getElectricalEfficiency();

	/*
	 * Get the thermal efficiency in [%] between 0 and 100%.
	 */
	public double getThermalEfficiency();

	/*
	 * Get the generated electrical energy in [kWh].
	 */
	public Value getElectricalEnergy() throws ComponentException;

	/*
	 * Get the generated thermal energy in [kWh].
	 */
	public Value getThermalEnergy() throws ComponentException;

	/*
	 * Get the generated electrical power in [W].
	 */
	public Value getElectricalPower() throws ComponentException;

	/*
	 * Get the generated thermal power in [W].
	 */
	public Value getThermalPower() throws ComponentException;

	/*
	 * Start the energy generation.
	 */
	public void start(Value value) throws EnergyManagementException;

	/*
	 * Stop the energy generation.
	 */
	public void stop(Long time) throws EnergyManagementException;

}
