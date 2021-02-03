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
package org.the.ems.cmpt.inv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
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
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
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
	
	private ServiceRegistration<ElectricalEnergyStorageService> storageRegistration;

	protected S storage;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration
	protected ChannelListener setpoint;
	protected volatile Value setpointValue = DoubleValue.emptyValue();

	protected ExternalPower external;
	protected ConsumptionPower conssumption;

	@Override
	public Value getSetpoint() throws ComponentException {
		return setpointValue;
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
	@Configuration(mandatory=false)
	public Value getImportEnergy() throws ComponentException { return getConfiguredValue("import_energy"); }

	@Override
	@Configuration(mandatory=false)
	public Value getExportEnergy() throws ComponentException { return getConfiguredValue("export_energy"); }

	@Override
	@Configuration(value="dc_energy", mandatory=false)
	public Value getInputEnergy() throws ComponentException { return getConfiguredValue("dc_energy"); }

	@Override
	@Configuration(value="dc_power", mandatory=false)
	public Value getInputPower() throws ComponentException { return getConfiguredValue("dc_power"); }

	@Override
	@Configuration(mandatory=false)
	public Value getActivePower() throws ComponentException { return getConfiguredValue("active_power"); }

	@Override
	@Configuration(mandatory=false)
	public Value getActivePowerL1() throws ComponentException { return getConfiguredValue("active_power_l1"); }

	@Override
	@Configuration(mandatory=false)
	public Value getActivePowerL2() throws ComponentException { return getConfiguredValue("active_power_l2"); }

	@Override
	@Configuration(mandatory=false)
	public Value getActivePowerL3() throws ComponentException { return getConfiguredValue("active_power_l3"); }

	@Override
	@Configuration(mandatory=false)
	public Value getReactivePower() throws ComponentException { return getConfiguredValue("reactive_power"); }

	@Override
	@Configuration(mandatory=false)
	public Value getReactivePowerL1() throws ComponentException { return getConfiguredValue("reactive_power_l1"); }

	@Override
	@Configuration(mandatory=false)
	public Value getReactivePowerL2() throws ComponentException { return getConfiguredValue("reactive_power_l2"); }

	@Override
	@Configuration(mandatory=false)
	public Value getReactivePowerL3() throws ComponentException { return getConfiguredValue("reactive_power_l3"); }

	@Override
	@Configuration(mandatory=false)
	public Value getVoltageL1() throws ComponentException { return getConfiguredValue("voltage_l1"); }

	@Override
	@Configuration(mandatory=false)
	public Value getVoltageL2() throws ComponentException { return getConfiguredValue("voltage_l2"); }

	@Override
	@Configuration(mandatory=false)
	public Value getVoltageL3() throws ComponentException { return getConfiguredValue("voltage_l3"); }

	@Override
	@Configuration(mandatory=false)
	public Value getFrequency() throws ComponentException { return getConfiguredValue("frequency"); }

	public S getElectricalEnergyStorage() {
		return storage;
	}

	@Override
    @SuppressWarnings("unchecked")
	protected void onActivate(BundleContext context, Map<String, ?> properties) throws ComponentException {
		super.onActivate(context, properties);
		
		Class<?> type = this.getClass();
		if (!type.equals(Inverter.class)) {
	        while (type.getSuperclass() != null) {
	            if (type.getSuperclass().equals(Inverter.class)) {
	                break;
	            }
	            type = type.getSuperclass();
	        }
			try {
		        // This operation is safe. Because clazz is a direct sub-class, getGenericSuperclass() will
		        // always return the Type of this class. Because this class is parameterized, the cast is safe
		        ParameterizedType superclass = (ParameterizedType) type.getGenericSuperclass();
		        Class<S> storageType = (Class<S>) superclass.getActualTypeArguments()[0];
		        storage = (S) storageType.getDeclaredConstructor().newInstance();
				
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				throw new ComponentException(e);
			}
		}
		else {
			storage = (S) new ElectricalEnergyStorage();
		}
		Map<String, Object> configs = new HashMap<String, Object>();
		configs.put("general.id", ((String) properties.get("general.id")).replace("inv", "ees"));
		configs.put("general.type", properties.get("general.type"));
		
		for (Entry<String, ?> config : properties.entrySet()) {
			if (config.getKey().startsWith("storage.")) {
				configs.put(config.getKey().replace("storage.", "general."), config.getValue());
			}
		}
		storage.activate(context, configs);
        storageRegistration = context.registerService(ElectricalEnergyStorageService.class, storage, 
				new Hashtable<String, Object>(configs));
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		external = new ExternalPower().activate(content).configure(configs).register(this);
		conssumption = new ConsumptionPower().activate(content).configure(configs).register(this);
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
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		setpoint.deregister();
		
		if (storageRegistration != null) {
			storageRegistration.unregister();
		}
		storage.deactivate();
		external.deactivate();
		conssumption.deactivate();
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
		if (this.setpoint.getLatestValue() == null || !this.setpoint.getLatestValue().equals(value)) {
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
