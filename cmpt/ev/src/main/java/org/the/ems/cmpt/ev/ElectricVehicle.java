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
package org.the.ems.cmpt.ev;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.Runnable;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Value;

@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ElectricVehicle extends Runnable implements ElectricVehicleService {

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration
	protected double capacity;

	@Override
	public double getCapacity() {
		return capacity;
	}

    @Override
    public double getStartPower() {
		return getMinPower();
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
	@Configuration(value="charge_energy", mandatory=false)
	public Value getChargedEnergy() throws ComponentException { return getConfiguredValue("charge_energy"); }

	@Override
	@Configuration(value="charge_power", mandatory=false)
	public Value getChargePower() throws ComponentException { return getConfiguredValue("charge_power"); }

	@Override
	@Configuration(value="soc", mandatory=false)
	public Value getStateOfCharge() throws ComponentException { return getConfiguredValue("soc"); }

}