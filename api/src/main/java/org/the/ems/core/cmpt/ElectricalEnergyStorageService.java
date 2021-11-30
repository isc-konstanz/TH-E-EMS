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

public interface ElectricalEnergyStorageService extends SchedulableService {

	static final String PID = "org.the.ems.cmpt.ees";

	@Override
	public default ComponentType getType() {
		return ComponentType.ELECTRICAL_ENERGY_STORAGE;
	};

	/*
	 * Get the capacity of the storage system in kilowatt hours [kWh] larger than 0.
	 */
	public double getCapacity();

	/*
	 * Get the maximum state of charge in percent [%] between 0 and 100% .
	 */
	public double getMaxStateOfCharge() throws ComponentException;

	/*
	 * Get the minimum state of charge in percent [%] between 0 and 100% .
	 */
	public double getMinStateOfCharge() throws ComponentException;

	public boolean hasMinStateOfCharge() throws ComponentException, InvalidValueException;
	/*
	 * Get the state of charge in percent [%] between 0 and 100%.
	 */
	public Value getStateOfCharge() throws ComponentException, InvalidValueException;

	/*
	 * Get the DC power in watts [W].
	 */
	public Value getPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the Voltage in volts [V].
	 */
	public Value getVoltage() throws ComponentException, InvalidValueException;

}
