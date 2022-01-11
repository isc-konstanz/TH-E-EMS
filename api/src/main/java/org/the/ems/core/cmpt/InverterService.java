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

public interface InverterService extends SchedulableService {

	public static final String PID = "org.the.ems.cmpt.inv";

	@Override
	public default ComponentType getType() {
		return ComponentType.INVERTER;
	};

	/*
	 * Get the {@link ElectricalEnergyStorageService}, related to this inverter.
	 * 
	 * @return the {@link ElectricalEnergyStorageService}, related to this inverter.
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the service
	 */
	public ElectricalEnergyStorageService getEnergyStorage() throws ComponentException;

	/*
	 * Set the inverter island mode, decoupling it from the grid.
	 * 
	 * @param enabled the flag whether the island mode should be enabled or not
	 * 
	 * @throws UnsupportedOperationException if this inverter does not support the island mode
	 */
	public boolean setIsland(boolean enabled) throws UnsupportedOperationException;

	/*
	 * Get if the inverter is in island mode, decoupled from the grid.
	 * 
	 * @return whether the island mode is enabled or not
	 * 
	 * @throws UnsupportedOperationException if this inverter does not support the island mode
	 */
	public boolean isIsland() throws UnsupportedOperationException;

	/*
	 * Get the maximum power in watts [W].
	 * 
	 * @return the maximum power
	 */
	public double getMaxPower();

	/*
	 * Get the minimum power in watts [W].
	 * 
	 * @return the minimum power
	 */
	public double getMinPower();

	/*
	 * Get the setpoint power value to control the import/export power in watts [W].
	 * 
	 * @return the setpoint power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getSetpoint() throws ComponentException, InvalidValueException;

	/*
	 * Get the setpoint power value to control the import/export power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new setpoint values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the setpoint power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getSetpoint(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new setpoint values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerSetpointListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new setpoint values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterSetpointListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the generated DC input energy in kilowatt hours [kWh].
	 * 
	 * @return the generated DC input energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getInputEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated DC input energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new DC input energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the generated DC input energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getInputEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new DC input energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerInputEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new DC input energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterInputEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the imported energy in kilowatt hours [kWh].
	 * 
	 * @return the imported energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getImportEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the imported energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the imported energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getImportEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerImportEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterImportEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the exported energy in kilowatt hours [kWh].
	 * 
	 * @return the exported energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getExportEnergy() throws ComponentException, InvalidValueException;

	/*
	 * Get the exported energy in kilowatt hours [kWh].
	 * Additionally, register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the exported energy {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getExportEnergy(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerExportEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new energy values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterExportEnergyListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the generated DC input power in watts [W].
	 * 
	 * @return the generated DC input power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getInputPower() throws ComponentException, InvalidValueException;

	/*
	 * Get the generated DC input power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new DC input power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the generated DC input power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getInputPower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new DC input power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerInputPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new DC input power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterInputPowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the active power in watts [W].
	 * 
	 * @return the active power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getActivePower() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power in watts [W].
	 * Additionally, register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the active power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getActivePower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerActivePowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterActivePowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Get the active power on L1 in watts [W].
	 * 
	 * @return the active power {@link Value} on L1
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getActivePowerL1() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power on L2 in watts [W].
	 * 
	 * @return the active power {@link Value} on L2
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getActivePowerL2() throws ComponentException, InvalidValueException;

	/*
	 * Get the active power on L3 in watts [W].
	 * 
	 * @return the active power {@link Value} on L3
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getActivePowerL3() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power in [var].
	 * 
	 * @return the reactive power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getReactivePower() throws ComponentException, InvalidValueException;

	/*
	 * Get the reactive power in [var].
	 * Additionally, register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the reactive power {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getReactivePower(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerReactivePowerListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new power values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterReactivePowerListener(ValueListener listener) throws ComponentException;

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
	 * Get the voltage in volt [V].
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

	/*
	 * Get the voltage on L1 in volt [V].
	 */
	public Value getVoltageL1() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage on L2 in volt [V].
	 */
	public Value getVoltageL2() throws ComponentException, InvalidValueException;

	/*
	 * Get the voltage on L3 in volt [V].
	 */
	public Value getVoltageL3() throws ComponentException, InvalidValueException;

	/*
	 * Get the frequency of the grid in hertz [Hz].
	 * 
	 * @return the frequency {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getFrequency() throws ComponentException, InvalidValueException;

	/*
	 * Get the frequency of the grid in hertz [Hz].
	 * Additionally, register a {@link ValueListener}, to be notified of new frequency values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @return the frequency {@link Value}
	 * 
	 * @throws ComponentException if any kind of error occurs retrieving the value
	 * @throws InvalidValueException if the retrieved value returned invalid
	 */
	public Value getFrequency(ValueListener listener) throws ComponentException, InvalidValueException;

	/*
	 * Register a {@link ValueListener}, to be notified of new frequency values.
	 * 
	 * @param listener the {@link ValueListener} to be notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs registering the listener
	 */
	public void registerFrequencyListener(ValueListener listener) throws ComponentException;

	/*
	 * Deregister a {@link ValueListener}, notified of new frequency values.
	 * 
	 * @param listener the {@link ValueListener} notified of values
	 * 
	 * @throws ComponentException if any kind of error occurs deregistering the listener
	 */
	public void deregisterFrequencyListener(ValueListener listener) throws ComponentException;

}
