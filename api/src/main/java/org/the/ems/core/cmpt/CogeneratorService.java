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
import org.the.ems.core.HeatingService;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public interface CogeneratorService extends HeatingService {

	public static final String PID = "org.the.ems.cmpt.chp";

	@Override
	public default ComponentType getType() {
		return ComponentType.COMBINED_HEAT_POWER;
	};

	/*
	 * Get the thermal efficiency in percent [%] between 0 and 100%.
	 * 
	 * @return the thermal efficiency in percent [%]
	 */
	public double getThermalEfficiency();

	/*
	 * Get the electrical efficiency in percent [%] between 0 and 100%.
	 * 
	 * @return the electrical efficiency in percent [%]
	 */
	public double getElectricalEfficiency();

	/*
	 * Get the generated electrical energy in kilowatt hours [kWh].
	 */
	public Value getElectricalEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated electrical energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new electrical energy values.
	 * 
	 * @return the generated electrical energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getElectricalEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new electrical energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerElectricalEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new electrical energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterElectricalEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the generated electrical power in watts [W].
	 */
	public Value getElectricalPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated electrical power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new electrical power values.
	 * 
	 * @return the generated electrical power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getElectricalPower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new electrical power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerElectricalPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new electrical power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterElectricalPowerListener(ValueListener listener) throws ComponentException;

}
