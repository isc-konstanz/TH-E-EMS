/* 
 * Copyright 2016-21 ISC Konstanz
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

public interface RunnableService extends SchedulableService {

	public RunState getState();

	/*
	 * Get the current runtime in milliseconds [ms].
	 */
	public int getRuntime();

	/*
	 * Get the minimum runtime in milliseconds [ms].
	 */
	public int getMinRuntime();

	/*
	 * Get the current idletime in milliseconds [ms].
	 */
	public int getIdletime();

	/*
	 * Get the minimum idletime in milliseconds [ms].
	 */
	public int getMinIdletime();

	/*
	 * Get the default value with which a component will be started with.
	 */
	public Value getStartValue(long time);

	/*
	 * Get the default value with which a component will be started with.
	 */
	public default Value getStartValue() {
		return getStartValue(System.currentTimeMillis());
	}

	/*
	 * Start the component.
	 */
	public void start(Value value) throws EnergyManagementException;

	/*
	 * Start the component.
	 */
	public default void start(long time) throws EnergyManagementException {
		Value value = getStartValue(time);
		start(value);
	}

	/*
	 * Start the component.
	 */
	public default void start() throws EnergyManagementException {
		start(System.currentTimeMillis());
	}

	/*
	 * Stop the component.
	 */
	public void stop(long time) throws EnergyManagementException;

	/*
	 * Stop the component.
	 */
	public default void stop() throws EnergyManagementException {
		stop(System.currentTimeMillis());
	}

}
