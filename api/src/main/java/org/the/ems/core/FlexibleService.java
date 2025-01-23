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

import org.the.ems.core.data.InvalidValueException;

public interface FlexibleService extends RunnableService {

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
	 * Get the default power with which a component will be started with in watts [W].
	 * 
	 * @return the default power to which a component will be started with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public double getStartPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the amount of startable electrical power in watts [W].
	 * Returns zero, if the component is already running.
	 * 
	 * @return the startable electrical power in watts [W]
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public double getStartablePower() throws ComponentException, InvalidValueException;

	/*
	 * Get the default value to which a component will be stopped to in watts [W].
	 * 
	 * @return the default power with which a component will be stopped to
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the settings
	 * @throws InvalidValueException if the settings retrieved an invalid value
	 */
	public double getStopPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the amount of stoppable electrical power in watts [W].
	 * Returns zero, if the component is not running.
	 * 
	 * @return the stoppable electrical power in watts [W]
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public double getStoppablePower() throws ComponentException, InvalidValueException;

}
