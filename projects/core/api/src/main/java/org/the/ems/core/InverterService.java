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

public interface InverterService extends ComponentService {

	@Override
	public default ComponentType getType() {
		return ComponentType.INVERTER;
	};

	/*
	 * Get the maximum power in [W].
	 */
	public double getMaxPower();

	/*
	 * Get the minimum power in [W].
	 */
	public double getMinPower();

	/*
	 * Get if the inverter is in island mode, decoupled from the grid.
	 */
	public boolean isIsland();

	/*
	 * Get the imported energy in [kWh].
	 */
	public Value getImportEnergy() throws ComponentException;

	/*
	 * Get the exported energy in [kWh].
	 */
	public Value getExportEnergy() throws ComponentException;

	/*
	 * Get the generated DC input energy in [kWh].
	 */
	public Value getGeneratedEnergy() throws ComponentException;

	/*
	 * Get the active power in [W].
	 */
	public Value getActivePower() throws ComponentException;

	/*
	 * Get the reactive power in [var].
	 */
	public Value getReactivePower() throws ComponentException;

	/*
	 * Get the generated DC input power in [W].
	 */
	public Value getGenerationPower() throws ComponentException;

}
