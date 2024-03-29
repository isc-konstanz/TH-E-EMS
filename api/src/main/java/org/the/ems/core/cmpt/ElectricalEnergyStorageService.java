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
import org.the.ems.core.data.ValueListener;

public interface ElectricalEnergyStorageService extends SchedulableService {

	public static final String PID = "org.the.ems.cmpt.ees";

	@Override
	public default ComponentType getType() {
		return ComponentType.ELECTRICAL_ENERGY_STORAGE;
	};

	/*
	 * Get the capacity of the storage system in kilowatt hours [kWh] larger than 0.
	 * 
	 * @return the capacity of the storage system
	 */
	public double getCapacity();

	/*
	 * Get the maximum state of charge in percent [%] between 0 and 100%.
	 * 
	 * @return the maximum state of charge of this storage system
	 */
	public double getMaxStateOfCharge() throws ComponentException;

	/*
	 * Get the minimum state of charge in percent [%] between 0 and 100%.
	 * 
	 * @return the minimum state of charge of this storage system
	 */
	public double getMinStateOfCharge() throws ComponentException;

	/*
	 * Get the state of charge in percent [%] between 0 and 100%.
	 * 
	 * @return the state of charge {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getStateOfCharge() throws ComponentException, InvalidValueException;

	/*
	 * Get the state of charge in percent [%] between 0 and 100%.
	 * Additionally, register a {@link ValueListener}, to be notified of new state values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the state of charge {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getStateOfCharge(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new state values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerStateOfChargeListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new state values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterStateOfChargeListener(ValueListener listener) throws ComponentException;

	/*
	 * Verify whether the storage can be charged.
	 * For instance, this may return false if the configured maximum state of charge
	 * is already reached.
	 * 
	 * @return the flag whether the storage can be charged
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public boolean isChargable() throws ComponentException, InvalidValueException;

	/*
	 * Verify whether the storage can be discharged.
	 * For instance, this may return false if the configured minimum state of charge
	 * is already reached.
	 * 
	 * @return the flag whether the storage can be discharged
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public boolean isDischargable() throws ComponentException, InvalidValueException;

	/*
	 * Get the DC power in watts [W].
	 * 
	 * @return the DC power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the DC power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the DC power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getPower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the Voltage in volts [V].
	 * 
	 * @return the voltage {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getVoltage() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage in volt [V].
	 * Additionally, register a {@link ValueListener}, to be notified of new voltage values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the voltage {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getVoltage(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new voltage values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerVoltageListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new voltage values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException;

}
