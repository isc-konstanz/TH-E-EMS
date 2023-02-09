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
package org.the.ems.cmpt.inv;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.cmpt.inv.data.ConsumptionPower;
import org.the.ems.cmpt.inv.data.ActivePower;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentContext;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;


public abstract class Inverter<S extends ElectricalEnergyStorage> extends Component 
		implements InverterService, InverterCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(Inverter.class);

	protected static final String IMPORT_ENERGY_VALUE = "import_energy";
	protected static final String IMPORT_POWER_VALUE = "import_power";

	protected static final String EXPORT_ENERGY_VALUE = "export_energy";
	protected static final String EXPORT_POWER_VALUE = "export_power";

	protected static final String INPUT_ENERGY_VALUE = "input_energy";
	protected static final String INPUT_POWER_VALUE = "input_power";

	protected static final String DC_ENERGY_VALUE = "dc_energy";
	protected static final String DC_POWER_VALUE = "dc_power";

	protected static final String AC_POWER_VALUE = "ac_power";
	protected static final String AC_POWER_L1_VALUE = "ac_power_l1";
	protected static final String AC_POWER_L2_VALUE = "ac_power_l2";
	protected static final String AC_POWER_L3_VALUE = "ac_power_l3";

	protected static final String ACTIVE_POWER_VALUE = "active_power";
	protected static final String ACTIVE_POWER_L1_VALUE = "active_power_l1";
	protected static final String ACTIVE_POWER_L2_VALUE = "active_power_l2";
	protected static final String ACTIVE_POWER_L3_VALUE = "active_power_l3";

	protected static final String REACTIVE_POWER_VALUE = "reactive_power";
	protected static final String REACTIVE_POWER_L1_VALUE = "reactive_power_l1";
	protected static final String REACTIVE_POWER_L2_VALUE = "reactive_power_l2";
	protected static final String REACTIVE_POWER_L3_VALUE = "reactive_power_l3";

	protected static final String VOLTAGE_VALUE = "voltage";
	protected static final String VOLTAGE_L1_VALUE = "voltage_l1";
	protected static final String VOLTAGE_L2_VALUE = "voltage_l2";
	protected static final String VOLTAGE_L3_VALUE = "voltage_l3";

	protected static final String FREQUENCY_VALUE = "frequency";

	protected S storage;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration(mandatory=false)
	protected Channel setpoint;
	protected volatile Value setpointValue = DoubleValue.zeroValue();

	protected final ActivePower external;
	protected final ConsumptionPower consumption;

	public Inverter() {
		super();
		external = new ActivePower();
		consumption = new ConsumptionPower(this);
	}

	@Override
	public ElectricalEnergyStorageService getEnergyStorage() throws ComponentException {
		if (storage == null) {
			throw new ComponentException("Electrical energy storage unavailable");
		}
		return storage;
	}

	@Override
	public boolean setIsland(boolean enabled) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isIsland() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMaxPower() {
		return powerMax;
	}

	@Override
	public double getMinPower() {
		return powerMin;
	}

	@Override
	public Value getSetpoint() throws ComponentException, InvalidValueException {
		return setpoint.getLatestValue();
	}

	@Override
	public Value getSetpoint(ValueListener listener) throws ComponentException, InvalidValueException {
		return setpoint.getLatestValue(listener);
	}

	@Override
	public void registerSetpointListener(ValueListener listener) throws ComponentException {
		setpoint.registerValueListener(listener);
	}

	@Override
	public void deregisterSetpointListener(ValueListener listener) throws ComponentException {
		setpoint.deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=IMPORT_ENERGY_VALUE, mandatory=false)
	public Value getImportEnergy() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(IMPORT_ENERGY_VALUE).getLatestValue();
	}

	@Override
	public Value getImportEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(IMPORT_ENERGY_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerImportEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(IMPORT_ENERGY_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterImportEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(IMPORT_ENERGY_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=EXPORT_ENERGY_VALUE, mandatory=false)
	public Value getExportEnergy() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(EXPORT_ENERGY_VALUE).getLatestValue();
	}

	@Override
	public Value getExportEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(EXPORT_ENERGY_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerExportEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(EXPORT_ENERGY_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterExportEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(EXPORT_ENERGY_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=INPUT_ENERGY_VALUE, mandatory=false)
	public Value getInputEnergy() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(INPUT_ENERGY_VALUE, DC_ENERGY_VALUE).getLatestValue();
	}

	@Override
	public Value getInputEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(INPUT_ENERGY_VALUE, DC_ENERGY_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerInputEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(INPUT_ENERGY_VALUE, DC_ENERGY_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterInputEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(INPUT_ENERGY_VALUE, DC_ENERGY_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=INPUT_POWER_VALUE, mandatory=false)
	public Value getInputPower() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(INPUT_POWER_VALUE, DC_POWER_VALUE).getLatestValue();
	}

	@Override
	public Value getInputPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(INPUT_POWER_VALUE, DC_POWER_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerInputPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(INPUT_POWER_VALUE, DC_POWER_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterInputPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(INPUT_POWER_VALUE, DC_POWER_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_VALUE, mandatory=false)
	public Value getActivePower() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ACTIVE_POWER_VALUE, AC_POWER_VALUE).getLatestValue();
	}

	@Override
	public Value getActivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ACTIVE_POWER_VALUE, AC_POWER_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerActivePowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ACTIVE_POWER_VALUE, AC_POWER_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterActivePowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ACTIVE_POWER_VALUE, AC_POWER_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L1_VALUE, mandatory=false)
	public Value getActivePowerL1() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ACTIVE_POWER_L1_VALUE, AC_POWER_L1_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L2_VALUE, mandatory=false)
	public Value getActivePowerL2() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ACTIVE_POWER_L2_VALUE, AC_POWER_L2_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L3_VALUE, mandatory=false)
	public Value getActivePowerL3() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ACTIVE_POWER_L3_VALUE, AC_POWER_L3_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=REACTIVE_POWER_VALUE, mandatory=false)
	public Value getReactivePower() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(REACTIVE_POWER_VALUE).getLatestValue();
	}

	@Override
	public Value getReactivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(REACTIVE_POWER_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerReactivePowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(REACTIVE_POWER_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterReactivePowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(REACTIVE_POWER_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L1_VALUE, mandatory=false)
	public Value getReactivePowerL1() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(REACTIVE_POWER_L1_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L2_VALUE, mandatory=false)
	public Value getReactivePowerL2() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(REACTIVE_POWER_L2_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L3_VALUE, mandatory=false)
	public Value getReactivePowerL3() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(REACTIVE_POWER_L3_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=VOLTAGE_VALUE, mandatory=false)
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(VOLTAGE_VALUE).getLatestValue();
	}

	@Override
	public Value getVoltage(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(VOLTAGE_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerVoltageListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(VOLTAGE_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(VOLTAGE_VALUE).deregisterValueListener(listener);
	}

	@Override
	@Configuration(value=VOLTAGE_L1_VALUE, mandatory=false)
	public Value getVoltageL1() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(VOLTAGE_L1_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=VOLTAGE_L2_VALUE, mandatory=false)
	public Value getVoltageL2() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(VOLTAGE_L2_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=VOLTAGE_L3_VALUE, mandatory=false)
	public Value getVoltageL3() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(VOLTAGE_L3_VALUE).getLatestValue();
	}

	@Override
	@Configuration(value=FREQUENCY_VALUE, mandatory=false)
	public Value getFrequency() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(FREQUENCY_VALUE).getLatestValue();
	}

	@Override
	public Value getFrequency(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(FREQUENCY_VALUE).getLatestValue(listener);
	}

	@Override
	public void registerFrequencyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(FREQUENCY_VALUE).registerValueListener(listener);
	}

	@Override
	public void deregisterFrequencyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(FREQUENCY_VALUE).deregisterValueListener(listener);
	}

	public S getElectricalEnergyStorage() {
		return storage;
	}

	@Override
    @SuppressWarnings("unchecked")
	protected void onActivate(ComponentContext context, Configurations configs) throws ComponentException {
		super.onActivate(context, configs);

		Class<S> storageClass;
		Class<?> thisClass = this.getClass();
		if (!thisClass.equals(Inverter.class)) {
	        while (thisClass.getSuperclass() != null) {
	            if (thisClass.getSuperclass().equals(Inverter.class)) {
	                break;
	            }
	            thisClass = thisClass.getSuperclass();
	        }
	        // This operation is safe. Because clazz is a direct sub-class, getGenericSuperclass() will
	        // always return the Type of this class. Because this class is parameterized, the cast is safe
	        ParameterizedType superclass = (ParameterizedType) thisClass.getGenericSuperclass();
	        storageClass = (Class<S>) superclass.getActualTypeArguments()[0];
		}
		else {
			storageClass =  (Class<S>) ElectricalEnergyStorage.class;
		}
		try {
			Constructor<S> constructor = storageClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			storage = constructor.newInstance();
        
	    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
	            | NoSuchMethodException | SecurityException e) {
	        throw new ComponentException(e);
	    }
		String id = getId().startsWith(getType().getKey()) ? 
					getId().replace(getType().getKey(), "ees") : "ees";
		getContext().registerService(id, configs, storage, ElectricalEnergyStorageService.class);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		getContext().registerService(getId().concat("_").concat("ext"), configs, external);
		getContext().registerService(getId().concat("_").concat("cons"), configs, consumption);
		if (setpoint != null) {
			setpoint.registerValueListener(new SetpointListener());
		}
	}

	@Override
	public void onResume() throws ComponentException {
		external.resume();
		consumption.resume();
	}

	@Override
	public void onPause() throws ComponentException {
		external.pause();
		consumption.pause();
	}

	@Override
    public final void schedule(Schedule schedule)
            throws UnsupportedOperationException, EnergyManagementException {
        
        if (isMaintenance()) {
            throw new MaintenanceException("Unable to schedule inverter while in maintenance");
        }
        doSchedule(schedule);
    }

    protected void doSchedule(Schedule schedule) 
            throws UnsupportedOperationException, EnergyManagementException {

        WriteContainer container = new WriteContainer();
        for (Value value : schedule) {
        	onSet(container, value);
        }
        onSchedule(container, schedule);
        
        write(container);
    }

    protected void onSchedule(WriteContainer container, Schedule schedule) 
            throws UnsupportedOperationException, ComponentException {
        // Default implementation to be overridden
    }

    @Override
    public final void set(Value value) 
            throws UnsupportedOperationException, EnergyManagementException {

		if (isMaintenance()) {
			throw new MaintenanceException();
		}
        doSet(value);
    }

    protected void doSet(Value value)
            throws UnsupportedOperationException, EnergyManagementException {

		if (value.doubleValue() > getMaxPower() || 
				value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid setpoint power value: {0}", value));
		}
        WriteContainer container = new WriteContainer();
        onSet(container, value);
        write(container);
    }

	public void onSet(WriteContainer container, Value value) throws ComponentException {
		if (setpoint == null) {
			throw new ComponentException(
					MessageFormat.format("Unable to configure setpoint for \"{0}\"", getClass().getSimpleName()));
		}
		double setpoint = value.doubleValue();
		if (setpoint > getMaxPower() || setpoint < getMinPower()) {
			throw new ComponentException("Inverter setpoint out of bounds: " + value);
		}
		try {
			if (!this.setpoint.getLatestValue().equals(value)) {
				this.setpoint.setLatestValue(value);
				return;
			}
		} catch (InvalidValueException e) {
			this.setpoint.setLatestValue(value);
			return;
		}
		if (external.isRunning()) {
			setpoint -= external.getExternalPower();
		}
		
		if (setpoint > getMaxPower()) {
			setpoint = getMaxPower();
		}
		else if (setpoint < getMinPower()) {
			setpoint = getMinPower();
		}
		onSetpointUpdate(container, new DoubleValue(setpoint, value.getEpochMillis()));
	}

	protected void onSetpointUpdate(WriteContainer container, Value value) throws ComponentException {
		// TODO: Verify setpoint import/export sign
	}

	@Override
	public void onSetpointUpdate() {
		try {
			set(setpointValue);
			
		} catch (EnergyManagementException e) {
			logger.debug("Unable to updating inverter setpoint: {}", e.getMessage());
		}
	}

	private class SetpointListener implements ValueListener {

		@Override
		public void onValueReceived(Value setpoint) {
	        if (setpointValue.doubleValue() != setpoint.doubleValue()) {
	            setpointValue = setpoint;
	            onSetpointUpdate();
	        }
		}
	}

}
