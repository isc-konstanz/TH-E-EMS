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
package org.the.ems.core.cmpt.ees;

import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Value;

public abstract class ElectricalEnergyStorage extends ConfiguredComponent
		implements ElectricalEnergyStorageService {

	@Configuration
	protected double capacity;

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	@Configuration
	public Value getChargePower() throws ComponentException { return getConfiguredValue("charge_power"); }

	@Override
	@Configuration
	public Value getVoltage() throws ComponentException { return getConfiguredValue("voltage"); }

	@Override
	@Configuration("soc")
	public Value getStateOfCharge() throws ComponentException { return getConfiguredValue("soc"); }

}
