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
package org.the.ems.core.cmpt.chp;

import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.GeneratorComponent;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Value;

public abstract class CogeneratorComponent extends GeneratorComponent implements CogeneratorService {

	@Configuration("th_eff")
	private double thermalEfficiency;

	@Configuration("el_eff")
	private double electricEfficiency;

	@Override
	public double getElectricalEfficiency() {
		return electricEfficiency;
	}

	@Override
	public double getThermalEfficiency() {
		return thermalEfficiency;
	}

	@Override
	@Configuration("el_energy")
	public Value getElectricalEnergy() throws ComponentException { return getConfiguredValue("el_energy"); }

	@Override
	@Configuration("th_energy")
	public Value getThermalEnergy() throws ComponentException { return getConfiguredValue("th_energy"); }

	@Override
	@Configuration("el_power")
	public Value getElectricalPower() throws ComponentException { return getConfiguredValue("el_power"); }

	@Override
	@Configuration("th_power")
	public Value getThermalPower() throws ComponentException { return getConfiguredValue("th_power"); }

}
