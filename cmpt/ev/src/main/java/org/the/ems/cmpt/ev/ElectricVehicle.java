/* 
 * Copyright 2016-2021 ISC Konstanz
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
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ElectricVehicle extends Runnable implements ElectricVehicleService {

	protected static final String CHARGE_ENERGY_VALUE = "charge_energy";
	protected static final String CHARGE_POWER_VALUE = "charge_power";
	protected static final String CHARGE_STATE_VALUE = "soc";

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000)
	protected double powerMin;

	@Configuration(mandatory=false)
	protected double capacity = Double.NaN;

	@Override
	public double getCapacity() throws ComponentException {
		if (capacity == Double.NaN) {
			throw new ComponentException("Unable to retrieve unconfigured capacity");
		}
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
	@Configuration(value=CHARGE_ENERGY_VALUE, mandatory=false)
	public Value getChargedEnergy() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_ENERGY_VALUE);
	}

	@Override
	@Configuration(value=CHARGE_POWER_VALUE, mandatory=false)
	public Value getChargePower() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_POWER_VALUE);
	}

	@Override
	@Configuration(value=CHARGE_STATE_VALUE, mandatory=false)
	public Value getStateOfCharge() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_STATE_VALUE);
	}

}
