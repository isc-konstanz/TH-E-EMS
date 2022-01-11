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
package org.the.ems.core;

import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public interface HeatingService extends RunnableService {

	/*
	 * Get the {@link ThermalEnergyStorageService}, related to this heating.
	 * 
	 * @return the {@link ThermalEnergyStorageService}, related to this heating.
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the service
	 */
	public ThermalEnergyStorageService getEnergyStorage() throws ComponentException;

	/*
	 * Get the active heating {@link Season}.
	 * Not every heating will support heating seasons.
	 * 
	 * @return the active season {@link Season}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the season
	 * @throws InvalidValueException if the retrieved season returned invalid
	 */
	public Season getSeason() throws ComponentException, InvalidValueException;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public default Value getStartValue(long time) {
		return new DoubleValue(getStartPower(), time);
	}

	/*
	 * Get the default power with which a component will be started with in watts [W].
	 * 
	 * @return the default power to which a component will be stopped to
	 */
	public double getStartPower();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public default Value getStopValue(long time) {
		return new DoubleValue(getStopPower(), time);
	}

	/*
	 * Get the default value to which a component will be stopped to in watts [W].
	 * 
	 * @return the default power with which a component will be started with
	 */
	public double getStopPower();

	/*
	 * Get the minimum power of this heating component in watts [W].
	 * 
	 * @return the minimum power of this heating component
	 */
	public double getMinPower();

	/*
	 * Get the maximum power of this heating component in watts [W].
	 * 
	 * @return the maximum power of this heating component
	 */
	public double getMaxPower();

	/*
	 * Get the generated thermal energy in kilowatt hours [kWh].
	 * 
	 * @return the generated thermal energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getThermalEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated thermal energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new thermal energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the generated thermal energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getThermalEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new thermal energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerThermalEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new thermal energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterThermalEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the generated thermal power in watts [W].
	 * 
	 * @return the generated thermal power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getThermalPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated thermal power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new thermal power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the generated thermal power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getThermalPower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new thermal power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerThermalPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new thermal power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterThermalPowerListener(ValueListener listener) throws ComponentException;

}
