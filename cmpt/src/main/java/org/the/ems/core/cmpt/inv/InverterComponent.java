/* 
 * Copyright 2016-19 ISC Konstanz
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
package org.the.ems.core.cmpt.inv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.cmpt.inv.cons.Consumption;
import org.the.ems.core.cmpt.inv.ext.ExternalSolar;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public abstract class InverterComponent extends ConfiguredComponent
		implements InverterService, InverterCallbacks, ValueListener {

	private static final Logger logger = LoggerFactory.getLogger(InverterComponent.class);

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration
	protected ChannelListener command;
	protected volatile Value commandValue = DoubleValue.emptyValue();

	protected Consumption cons;
	protected ExternalSolar solar;

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		
		cons = new Consumption(context, configs).register(this);
		solar = new ExternalSolar(context, configs).register(this);
		command.registerValueListener(this);
	}

	@Override
	public void onResume() throws EnergyManagementException {
		cons.resume();
		solar.resume();
	}

	@Override
	public void onPause() throws EnergyManagementException {
		cons.pause();
		solar.pause();
	}

	@Override
	public void onDeactivate() {
		cons.deactivate();
		solar.deactivate();
		command.deregister();
	}

	@Override
    public void onValueReceived(Value setpoint) {
        if (commandValue.doubleValue() != setpoint.doubleValue()) {
            commandValue = setpoint;
            onSetpointUpdate();
        }
    }

	@Override
	public void onSetpointUpdate() {
		try {
			WriteContainer container = new WriteContainer();
			
			onSet(container, commandValue);
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

	@Override
	public void onSet(WriteContainer container, Value value) throws ComponentException {
		if (value.doubleValue() != commandValue.doubleValue()) {
			command.setLatestValue(value);
			return;
		}
		if (value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException("Inverter setpoint out of bounds: " + value);
		}
		double setpoint = value.doubleValue();
		if (solar.isRunning()) {
			setpoint += solar.getSolar().doubleValue();
		}
		
		if (setpoint > getMaxPower()) {
			setpoint = getMaxPower();
		}
		else if (setpoint < getMinPower()) {
			setpoint = getMinPower();
		}
		onSetpointChanged(container, new DoubleValue(setpoint, value.getTime()));
	}

	@Override
	public void onSetpointChanged(Value value) throws EnergyManagementException { set(value); }

	protected abstract void onSetpointChanged(WriteContainer container, Value value) throws ComponentException;

	@Override
	public Value getCommand() throws ComponentException {
		return commandValue;
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
	@Configuration("dc_energy")
	public Value getInputEnergy() throws ComponentException { return getConfiguredValue("dc_energy"); }

	@Override
	@Configuration
	public Value getImportEnergy() throws ComponentException { return getConfiguredValue("import_energy"); }

	@Override
	@Configuration
	public Value getExportEnergy() throws ComponentException { return getConfiguredValue("export_energy"); }

	@Override
	@Configuration("dc_power")
	public Value getInputPower() throws ComponentException { return getConfiguredValue("dc_power"); }

	@Override
	@Configuration
	public Value getActivePower() throws ComponentException { return getConfiguredValue("active_power"); }

	@Override
	@Configuration
	public Value getActivePowerL1() throws ComponentException { return getConfiguredValue("active_power_l1"); }

	@Override
	@Configuration
	public Value getActivePowerL2() throws ComponentException { return getConfiguredValue("active_power_l2"); }

	@Override
	@Configuration
	public Value getActivePowerL3() throws ComponentException { return getConfiguredValue("active_power_l3"); }

	@Override
	@Configuration
	public Value getReactivePower() throws ComponentException { return getConfiguredValue("reactive_power"); }

	@Override
	@Configuration
	public Value getReactivePowerL1() throws ComponentException { return getConfiguredValue("reactive_power_l1"); }

	@Override
	@Configuration
	public Value getReactivePowerL2() throws ComponentException { return getConfiguredValue("reactive_power_l2"); }

	@Override
	@Configuration
	public Value getReactivePowerL3() throws ComponentException { return getConfiguredValue("reactive_power_l3"); }

	@Override
	@Configuration
	public Value getVoltageL1() throws ComponentException { return getConfiguredValue("voltage_l1"); }

	@Override
	@Configuration
	public Value getVoltageL2() throws ComponentException { return getConfiguredValue("voltage_l2"); }

	@Override
	@Configuration
	public Value getVoltageL3() throws ComponentException { return getConfiguredValue("voltage_l3"); }

	@Override
	@Configuration
	public Value getFrequency() throws ComponentException { return getConfiguredValue("frequency"); }

}
