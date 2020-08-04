/* 
 * Copyright 2016-20 ISC Konstanz
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

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.ext.ConsumptionPower;
import org.the.ems.cmpt.inv.ext.ExternalPower;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
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

@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Inverter extends Component implements InverterService, InverterCallbacks, ValueListener {
	private static final Logger logger = LoggerFactory.getLogger(Inverter.class);

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected ElectricalEnergyStorageService battery;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration
	protected Channel setpoint;

	@Configuration
	protected ChannelListener setpointPower;
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
	@Configuration(value="dc_energy", mandatory=false)
	public Value getInputEnergy() throws ComponentException { return getConfiguredValue("dc_energy"); }

	@Override
	@Configuration
	public Value getImportEnergy() throws ComponentException { return getConfiguredValue("import_energy"); }

	@Override
	@Configuration
	public Value getExportEnergy() throws ComponentException { return getConfiguredValue("export_energy"); }

	@Override
	@Configuration(value="dc_power", mandatory=false)
	public Value getInputPower() throws ComponentException { return getConfiguredValue("dc_power"); }

	@Override
	@Configuration
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

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		external = new ExternalPower().activate(content).configure(configs).register(this);
		conssumption = new ConsumptionPower().activate(content).configure(configs).register(this);
		setpointPower.registerValueListener(this);
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
	public void onDeactivate() {
		external.deactivate();
		conssumption.deactivate();
		setpointPower.deregister();
	}

	@Override
	public void onSet(WriteContainer container, Value value) throws ComponentException {
		double setpoint = value.doubleValue();
		if (setpoint > getMaxPower() || setpoint < getMinPower()) {
			throw new ComponentException("Inverter setpoint out of bounds: " + value);
		}
		if (setpoint != setpointValue.doubleValue()) {
			setpointPower.setLatestValue(value);
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
		
		double soc = battery.getStateOfCharge().doubleValue();
		if (soc < battery.getMinStateOfCharge() || soc > battery.getMaxStateOfCharge()) {
			if (this.setpoint.getLatestValue().doubleValue() != 0) {
				container.add(this.setpoint, new DoubleValue(0));
			}
			logger.debug("Requested inverter setpoint not allowed for Battery State of Charge of {}%", soc);
			return;
		}
		onSetpointChanged(container, new DoubleValue(setpoint, value.getTime()));
	}

	protected void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		// TODO: Verify setpoint import/export sign
		container.add(this.setpoint, value);
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

	@Override
    public void onValueReceived(Value setpoint) {
        if (setpointValue.doubleValue() != setpoint.doubleValue()) {
            setpointValue = setpoint;
            onSetpointUpdate();
        }
    }

}
