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
import org.the.ems.core.RunnableService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public interface ApplianceService extends RunnableService {

	public static final String PID = "org.the.ems.cmpt.apl";

	@Override
	public default ComponentType getType() {
		return ComponentType.APPLIANCE;
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public default Value getStartValue(long time) {
		return new DoubleValue(getStartPower(), time);
	}

	/*
	 * Get the default power with which an appliance will be started with in watts [W].
	 * 
	 * @return the default power with which an appliance will be started with
	 */
	public double getStartPower();

	/*
	 * Get the minimum power of this appliance in watts [W].
	 * 
	 * @return the minimum power of this appliance component
	 */
	public double getMinPower();

	/*
	 * Get the maximum power of this appliance in watts [W].
	 * 
	 * @return the maximum power of this appliance component
	 */
	public double getMaxPower();

	/*
	 * Get the consumed energy in kilowatt hours [kWh].
	 * 
	 * @return the consumed energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the consumed energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @return the consumed energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the consumption power in watts [W].
	 * 
	 * @return the consumed power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the consumed power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @return the consumed power {@link Value}
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

}
