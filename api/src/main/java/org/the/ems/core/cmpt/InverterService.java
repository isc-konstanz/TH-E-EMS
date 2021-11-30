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
package org.the.ems.core.cmpt;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.SchedulableService;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

public interface InverterService extends SchedulableService {

	static final String PID = "org.the.ems.cmpt.inv";

	@Override
	public default ComponentType getType() {
		return ComponentType.INVERTER;
	};

	/*
	 * Set the inverter island mode, decoupling it from the grid.
	 */
	public boolean setIsland(boolean enabled) throws UnsupportedOperationException;

	/*
	 * Get if the inverter is in island mode, decoupled from the grid.
	 */
	public boolean isIsland() throws UnsupportedOperationException;
	
	public ElectricalEnergyStorageService getStorage();

	/*
	 * Get the maximum power in watts [W].
	 */
	public double getMaxPower();

	/*
	 * Get the minimum power in watts [W].
	 */
	public double getMinPower();

	/*
	 * Get the setpoint power value to control the import/export power in watts [W].
	 */
	public Value getSetpoint() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated DC input energy in kilowatt hours [kWh].
	 */
	public Value getInputEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the imported energy in kilowatt hours [kWh].
	 */
	public Value getImportEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the exported energy in kilowatt hours [kWh].
	 */
	public Value getExportEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated DC input power in watts [W].
	 */
	public Value getInputPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power in [W].
	 */
	public Value getActivePower() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power on L1 in [var].
	 */
	public Value getActivePowerL1() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power on L2 in [var].
	 */
	public Value getActivePowerL2() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power on L3 in [var].
	 */
	public Value getActivePowerL3() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power in [var].
	 */
	public Value getReactivePower() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power on L1 in [var].
	 */
	public Value getReactivePowerL1() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power on L2 in [var].
	 */
	public Value getReactivePowerL2() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power on L3 in [var].
	 */
	public Value getReactivePowerL3() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage on L1 in [V].
	 */
	public Value getVoltageL1() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage on L2 in [V].
	 */
	public Value getVoltageL2() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage on L3 in [V].
	 */
	public Value getVoltageL3() throws ComponentException, InvalidValueException;

	/*
	 * Get the frequency of the grid in [Hz].
	 */
	public Value getFrequency() throws ComponentException, InvalidValueException;

}
