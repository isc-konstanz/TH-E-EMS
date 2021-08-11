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
package org.the.ems.cmpt.ees;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;


@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricalEnergyStorageService.class,
	configurationPid = ElectricalEnergyStorageService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ElectricalEnergyStorage extends Component implements ElectricalEnergyStorageService {

	@Configuration(mandatory=false)
	private double socMax = 100;

	@Configuration(mandatory=false)
	private double socMin = 0;

	@Configuration
	protected double capacity;

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public double getMaxStateOfCharge() {
		return socMax;
	}

	@Override
	public double getMinStateOfCharge() {
		return socMin;
	}

	@Override
	@Configuration("soc")
	public Value getStateOfCharge() throws ComponentException { return getConfiguredValue("soc"); }

	@Override
	@Configuration(mandatory=false, value="power")
	public Value getChargePower() throws ComponentException { return getConfiguredValue("power"); }

	@Override
	@Configuration(mandatory=false)
	public Value getVoltage() throws ComponentException { return getConfiguredValue("voltage"); }

	public void activate(BundleContext context, Map<String, ?> properties) throws ComponentException {
		super.doActivate(context, properties);
	}

	public void deactivate() throws ComponentException {
		super.doDeactivate();
	}

    @Override
    public final void schedule(Schedule schedule)
            throws UnsupportedOperationException, EnergyManagementException {
        
        if (isMaintenance()) {
            throw new MaintenanceException("Unable to schedule battery while in maintenance");
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

    protected void onSet(WriteContainer container, Value value)
            throws UnsupportedOperationException, ComponentException {
        // Default implementation to be overridden
        throw new UnsupportedOperationException();
    }

    protected void doWrite(WriteContainer container) throws EnergyManagementException {
        if (container.size() < 1) {
            return;
        }
        for (Channel channel : container.keySet()) {
            channel.write(container.get(channel));
        }
    }

}
