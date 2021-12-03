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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.cmpt.inv.ext.ConsumptionPower;
import org.the.ems.cmpt.inv.ext.ExternalPower;
import org.the.ems.core.Component;
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


@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Inverter<S extends ElectricalEnergyStorage> extends Component 
		implements InverterService, InverterCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(Inverter.class);

	protected static final String IMPORT_ENERGY_VALUE = "import_energy";
	protected static final String IMPORT_POWER_VALUE = "import_power";

	protected static final String EXPORT_ENERGY_VALUE = "export_energy";
	protected static final String EXPORT_POWER_VALUE = "export_power";

	protected static final String DC_ENERGY_VALUE = "dc_energy";
	protected static final String DC_POWER_VALUE = "ac_power";

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

	@Configuration
	protected Channel setpoint;
	protected volatile Value setpointValue = DoubleValue.emptyValue();

	protected final ExternalPower external;
	protected final ConsumptionPower conssumption;

	public Inverter() {
		super();
		external = new ExternalPower();
		conssumption = new ConsumptionPower();
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
		return getConfiguredValue(IMPORT_ENERGY_VALUE);
	}

	@Override
	public Value getImportEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(IMPORT_ENERGY_VALUE, listener);
	}

	@Override
	public void registerImportEnergyListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(IMPORT_ENERGY_VALUE, listener);
	}

	@Override
	public void deregisterImportEnergyListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(IMPORT_ENERGY_VALUE, listener);
	}

	@Override
	@Configuration(value=EXPORT_ENERGY_VALUE, mandatory=false)
	public Value getExportEnergy() throws ComponentException, InvalidValueException {
		return getConfiguredValue(EXPORT_ENERGY_VALUE);
	}

	@Override
	public Value getExportEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(EXPORT_ENERGY_VALUE, listener);
	}

	@Override
	public void registerExportEnergyListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(EXPORT_ENERGY_VALUE, listener);
	}

	@Override
	public void deregisterExportEnergyListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(EXPORT_ENERGY_VALUE, listener);
	}

	@Override
	@Configuration(value=DC_ENERGY_VALUE, mandatory=false)
	public Value getInputEnergy() throws ComponentException, InvalidValueException {
		return getConfiguredValue(DC_ENERGY_VALUE);
	}

	@Override
	public Value getInputEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(DC_ENERGY_VALUE, listener);
	}

	@Override
	public void registerInputEnergyListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(DC_ENERGY_VALUE, listener);
	}

	@Override
	public void deregisterInputEnergyListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(DC_ENERGY_VALUE, listener);
	}

	@Override
	@Configuration(value=DC_POWER_VALUE, mandatory=false)
	public Value getInputPower() throws ComponentException, InvalidValueException {
		return getConfiguredValue(DC_POWER_VALUE);
	}

	@Override
	public Value getInputPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(DC_POWER_VALUE, listener);
	}

	@Override
	public void registerInputPowerListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(DC_POWER_VALUE, listener);
	}

	@Override
	public void deregisterInputPowerListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(DC_POWER_VALUE, listener);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_VALUE, mandatory=false)
	public Value getActivePower() throws ComponentException, InvalidValueException {
		return getConfiguredValue(ACTIVE_POWER_VALUE);
	}

	@Override
	public Value getActivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(ACTIVE_POWER_VALUE, listener);
	}

	@Override
	public void registerActivePowerListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(ACTIVE_POWER_VALUE, listener);
	}

	@Override
	public void deregisterActivePowerListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(ACTIVE_POWER_VALUE, listener);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L1_VALUE, mandatory=false)
	public Value getActivePowerL1() throws ComponentException, InvalidValueException {
		return getConfiguredValue(ACTIVE_POWER_L1_VALUE);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L2_VALUE, mandatory=false)
	public Value getActivePowerL2() throws ComponentException, InvalidValueException {
		return getConfiguredValue(ACTIVE_POWER_L2_VALUE);
	}

	@Override
	@Configuration(value=ACTIVE_POWER_L3_VALUE, mandatory=false)
	public Value getActivePowerL3() throws ComponentException, InvalidValueException {
		return getConfiguredValue(ACTIVE_POWER_L3_VALUE);
	}

	@Override
	@Configuration(value=REACTIVE_POWER_VALUE, mandatory=false)
	public Value getReactivePower() throws ComponentException, InvalidValueException {
		return getConfiguredValue(REACTIVE_POWER_VALUE);
	}

	@Override
	public Value getReactivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(REACTIVE_POWER_VALUE, listener);
	}

	@Override
	public void registerReactivePowerListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(REACTIVE_POWER_VALUE, listener);
	}

	@Override
	public void deregisterReactivePowerListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(REACTIVE_POWER_VALUE, listener);
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L1_VALUE, mandatory=false)
	public Value getReactivePowerL1() throws ComponentException, InvalidValueException {
		return getConfiguredValue(REACTIVE_POWER_L1_VALUE);
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L2_VALUE, mandatory=false)
	public Value getReactivePowerL2() throws ComponentException, InvalidValueException {
		return getConfiguredValue(REACTIVE_POWER_L2_VALUE);
	}

	@Override
	@Configuration(value=REACTIVE_POWER_L3_VALUE, mandatory=false)
	public Value getReactivePowerL3() throws ComponentException, InvalidValueException {
		return getConfiguredValue(REACTIVE_POWER_L3_VALUE);
	}

	@Override
	@Configuration(value=VOLTAGE_VALUE, mandatory=false)
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_VALUE);
	}

	@Override
	public Value getVoltage(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_VALUE, listener);
	}

	@Override
	public void registerVoltageListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(VOLTAGE_VALUE, listener);
	}

	@Override
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(VOLTAGE_VALUE, listener);
	}

	@Override
	@Configuration(value=VOLTAGE_L1_VALUE, mandatory=false)
	public Value getVoltageL1() throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_L1_VALUE);
	}

	@Override
	@Configuration(value=VOLTAGE_L2_VALUE, mandatory=false)
	public Value getVoltageL2() throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_L2_VALUE);
	}

	@Override
	@Configuration(value=VOLTAGE_L3_VALUE, mandatory=false)
	public Value getVoltageL3() throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_L3_VALUE);
	}

	@Override
	@Configuration(value=FREQUENCY_VALUE, mandatory=false)
	public Value getFrequency() throws ComponentException, InvalidValueException {
		return getConfiguredValue(FREQUENCY_VALUE);
	}

	@Override
	public Value getFrequency(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(FREQUENCY_VALUE, listener);
	}

	@Override
	public void registerFrequencyListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(FREQUENCY_VALUE, listener);
	}

	@Override
	public void deregisterFrequencyListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(FREQUENCY_VALUE, listener);
	}

	public S getElectricalEnergyStorage() {
		return storage;
	}

	@Override
    @SuppressWarnings("unchecked")
	protected void onActivate(BundleContext context, Configurations configs) throws ComponentException {
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
		
		registerService(id, configs, storage, ElectricalEnergyStorageService.class);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		registerService(getId().concat("_").concat("ext"), configs, external);
		registerService(getId().concat("_").concat("cons"), configs, conssumption);
		setpoint.registerValueListener(new SetpointListener());
	}

	@Override
	public void onResume() throws ComponentException {
		external.resume();
		conssumption.resume();
	}

	@Override
	public void onPause() throws ComponentException {
		external.pause();
		conssumption.pause();
	}

	@Override
    public final void schedule(Schedule schedule)
            throws UnsupportedOperationException, EnergyManagementException {
        
        if (isMaintenance()) {
            throw new MaintenanceException("Unable to schedule inverter while in maintenance");
        }
        WriteContainer container = new WriteContainer();
        for (Value value : schedule) {
            doSet(container, value);
        }
        doWrite(container);
    }

    protected void doSchedule(WriteContainer container, Schedule schedule) 
            throws UnsupportedOperationException, EnergyManagementException {
        
        for (Value value : schedule) {
            doSet(container, value);
        }
        onSchedule(container, schedule);
    }

    protected void onSchedule(WriteContainer container, Schedule schedule) 
            throws UnsupportedOperationException, ComponentException {
        // Default implementation to be overridden
    }

    @Override
    public final void set(Value value) 
            throws UnsupportedOperationException, EnergyManagementException {
        
        WriteContainer container = new WriteContainer();
        doSet(container, value);
        doWrite(container);
    }

    protected void doSet(WriteContainer container, Value value)
            throws UnsupportedOperationException, EnergyManagementException {
        
        onSet(container, value);
    }

	public void onSet(WriteContainer container, Value value) throws ComponentException {
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
			setpoint += external.getSolar().doubleValue();
		}
		
		if (setpoint > getMaxPower()) {
			setpoint = getMaxPower();
		}
		else if (setpoint < getMinPower()) {
			setpoint = getMinPower();
		}
		
//		double soc = storage.getStateOfCharge().doubleValue();
//		if (soc < storage.getMinStateOfCharge() || soc > storage.getMaxStateOfCharge()) {
//			if (this.setpoint.getLatestValue().doubleValue() != 0) {
//				container.add(this.setpoint, new DoubleValue(0));
//			}
//			logger.debug("Requested inverter setpoint not allowed for Battery State of Charge of {}%", soc);
//			return;
//		}
		onSetpointChanged(container, new DoubleValue(setpoint, value.getTime()));
	}

    protected void doWrite(WriteContainer container) throws EnergyManagementException {
        if (container.size() < 1) {
            return;
        }
        for (Channel channel : container.keySet()) {
            channel.write(container.get(channel));
        }
    }

	protected void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		// TODO: Verify setpoint import/export sign
	}

	@Override
	public void onSetpointChanged(Value value) throws EnergyManagementException { set(value); }

	@Override
	public void onSetpointUpdate() {
		try {
			WriteContainer container = new WriteContainer();
			
			doSet(container, setpointValue);
			if (container.size() < 1) {
				return;
			}
			for (Channel channel : container.keySet()) {
				channel.write(container.get(channel));
			}
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