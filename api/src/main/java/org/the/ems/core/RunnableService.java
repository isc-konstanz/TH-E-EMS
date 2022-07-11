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
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;

public interface RunnableService extends SchedulableService {

	/*
	 * Get the current {@link RunState} of the component.
	 * 
	 * @return the current run state
	 */
	public RunState getState();

	/*
	 * Get the current {@link RunState} of the component.
	 * Additionally, register a {@link RunStateListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link RunStateListener} to be notified of states
	 * 
	 * @return the current run state
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the season
	 * @throws InvalidValueException if the retrieved season returned invalid
	 */
	public RunState getState(RunStateListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link RunStateListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link RunStateListener} to be notified of states
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerStateListener(RunStateListener listener) throws ComponentException;

	/*
	 * Deregister a {@link RunStateListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link RunStateListener} to be notified of states
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterStateListener(RunStateListener listener) throws ComponentException;

	/*
	 * Get the current state {@link Value} of the component.
	 * 
	 * @return the current state {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getStateValue() throws ComponentException, InvalidValueException;

	/*
	 * Get the current state {@link Value} of the component.
	 * Additionally, register a {@link ValueListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link ValueListener} to be notified of states
	 * 
	 * @return the current run state
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the season
	 * @throws InvalidValueException if the retrieved season returned invalid
	 */
	public Value getStateValue(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link ValueListener} to be notified of states
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerStateValueListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, to be notified of changed states.
	 * 
	 * @param listener the {@link ValueListener} to be notified of states
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterStateValueListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the current runtime in milliseconds [ms].
	 * 
	 * @return the current runtime
	 */
	public int getRuntime();

	/*
	 * Get the minimum runtime in milliseconds [ms].
	 * 
	 * @return the minimum runtime
	 */
	public int getMinRuntime();

	/*
	 * Get the current idletime in milliseconds [ms].
	 * 
	 * @return the current idletime
	 */
	public int getIdletime();

	/*
	 * Get the minimum idletime in milliseconds [ms].
	 * 
	 * @return the minimum idletime
	 */
	public int getMinIdletime();

	/*
	 * Get the default value with which a component will be started with.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return the default {@link Value} with which a component will be started with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getStartValue(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Get the default value with which a component will be started with.
	 * 
	 * @return the default {@link Value} with which a component will be started with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public default Value getStartValue() throws ComponentException, InvalidValueException {
		return getStartValue(System.currentTimeMillis());
	}

	/*
	 * Get the settings with which a component will be started with.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return the {@link StartSettings} with which a component will be started with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the settings
	 * @throws InvalidValueException if the settings retrieved an invalid value
	 */
	public StartSettings getStartSettings(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Get the settings with which a component will be started with.
	 * 
	 * @return the {@link StartSettings} with which a component will be started with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the settings
	 * @throws InvalidValueException if the settings retrieved an invalid value
	 */
	public default StartSettings getStartSettings() throws ComponentException, InvalidValueException {
		return getStartSettings(System.currentTimeMillis());
	}

	/*
	 * Verify whether the component can be started.
	 * For instance, this may return false if the minimum idle time was not yet passed.
	 * 
	 * @return the flag whether the component can be started
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public default boolean isStartable() throws ComponentException, InvalidValueException {
		return isStartable(System.currentTimeMillis());
	}

	/*
	 * Verify whether the component can be started.
	 * For instance, this may return false if the minimum idle time was not yet passed.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return whether the component can be started
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public boolean isStartable(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Start the component.
	 * 
	 * @param settings the settings for the component to be started with
	 * 
	 * @throws EnergyManagementException if any kind of error occurs starting the component
	 */
	public void start(StartSettings settings) throws EnergyManagementException;

	/*
	 * Start the component.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @throws EnergyManagementException if any kind of error occurs starting the component
	 */
	public void start(long timestamp) throws EnergyManagementException;

	/*
	 * Start the component.
	 * 
	 * @throws EnergyManagementException if any kind of error occurs starting the component
	 */
	public default void start() throws EnergyManagementException {
		start(System.currentTimeMillis());
	}

	/*
	 * Get the default value to which a component will be stopped to.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return the default {@link Value} to which a component will be stopped to
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getStopValue(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Get the default value to which a component will be stopped to.
	 * 
	 * @return the default {@link Value} to which a component will be stopped to
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public default Value getStopValue() throws ComponentException, InvalidValueException {
		return getStopValue(System.currentTimeMillis());
	}

	/*
	 * Get the settings with which a component will be stopped with.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return the {@link StopSettings} with which a component will be stopped with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the settings
	 * @throws InvalidValueException if the settings retrieved an invalid value
	 */
	public StopSettings getStopSettings(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Get the settings with which a component will be stopped with.
	 * 
	 * @return the {@link StopSettings} with which a component will be stopped with
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the settings
	 * @throws InvalidValueException if the settings retrieved an invalid value
	 */
	public default StopSettings getStopSettings() throws ComponentException, InvalidValueException {
		return getStopSettings(System.currentTimeMillis());
	}

	/*
	 * Verify whether the component can be stopped.
	 * For instance, this may return false if the minimum runtime was not yet passed.
	 * 
	 * @return the flag whether the component can be stopped
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public default boolean isStoppable() throws ComponentException, InvalidValueException {
		return isStoppable(System.currentTimeMillis());
	}

	/*
	 * Verify whether the component can be stopped.
	 * For instance, this may return false if the minimum runtime was not yet passed.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @return whether the component can be stopped
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public boolean isStoppable(long timestamp) throws ComponentException, InvalidValueException;

	/*
	 * Stop the component.
	 * 
	 * @param settings the settings for the component to be stopped with
	 * 
	 * @throws EnergyManagementException if any kind of error occurs starting the component
	 */
	public void stop(StopSettings settings) throws EnergyManagementException;

	/*
	 * Stop the component.
	 * 
	 * @param timestamp the execution time as a UNIX timestamp
	 * 
	 * @throws EnergyManagementException if any kind of error occurs stopping the component
	 */
	public void stop(long timestamp) throws EnergyManagementException;

	/*
	 * Stop the component.
	 * 
	 * @throws EnergyManagementException if any kind of error occurs stopping the component
	 */
	public default void stop() throws EnergyManagementException {
		stop(System.currentTimeMillis());
	}

}
