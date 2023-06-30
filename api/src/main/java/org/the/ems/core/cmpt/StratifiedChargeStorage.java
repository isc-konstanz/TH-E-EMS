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
import org.the.ems.core.HeatingType;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public interface StratifiedChargeStorage extends ThermalEnergyStorageService {

	/*
	 * Get the temperature of a certain layer in degree celsius [°C].
	 * 
	 * @param type the {@link HeatingType} to addressed
	 * 
	 * @return the average temperature {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getTemperature(HeatingType type) throws ComponentException, InvalidValueException;

	/*
	 * Get the temperature of a certain layer in degree celsius [°C].
	 * Additionally, register a {@link ValueListener}, to be notified of new temperature values.
	 * 
	 * @param type the {@link HeatingType} to addressed
	 * 
	 * @return the average temperature {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getTemperature(HeatingType type, ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new temperature values of a certain layer.
	 * 
	 * @param type the {@link HeatingType} to addressed
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerTemperaturereListener(HeatingType type, ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new temperature values of a certain layer.
	 * 
	 * @param type the {@link HeatingType} to addressed
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterTemperatureListener(HeatingType type, ValueListener listener) throws ComponentException;

}
