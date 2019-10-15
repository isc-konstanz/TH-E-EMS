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
package org.the.ems.cmpt.chp;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.Generator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Cogenerator extends Generator implements CogeneratorService {

	@Configuration("th_eff")
	protected double thermalEfficiency;

	@Configuration("el_eff")
	protected double electricEfficiency;

	@Configuration(mandatory = false)
	protected boolean starter = false;

	@Configuration(mandatory = false)
	protected int starterDelay = 2500;

	@Configuration(mandatory = false)
	protected int enableDelay = 5000;

	@Configuration
	protected Channel enable;

	@Configuration
	protected Channel state;

	@Override
	public double getElectricalEfficiency() {
		return electricEfficiency;
	}

	@Override
	public double getThermalEfficiency() {
		return thermalEfficiency;
	}

	@Override
	@Configuration(value="el_energy", mandatory=false)
	public Value getElectricalEnergy() throws ComponentException { return getConfiguredValue("el_energy"); }

	@Override
	@Configuration(value="th_energy", mandatory=false)
	public Value getThermalEnergy() throws ComponentException { return getConfiguredValue("th_energy"); }

	@Override
	@Configuration(value="el_power", mandatory=false)
	public Value getElectricalPower() throws ComponentException { return getConfiguredValue("el_power"); }

	@Override
	@Configuration(value="th_power", mandatory=false)
	public Value getThermalPower() throws ComponentException { return getConfiguredValue("th_power"); }

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
		long delay = time + enableDelay;
		
		container.addBoolean(enable, true, time);
		container.addBoolean(state, true, delay);
		if (starter) {
			delay += starterDelay;
			container.addBoolean(state, false, delay);
		}
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		long delay = time;
		if (!starter || state.getLatestValue().booleanValue()) {
			delay += enableDelay;
			container.addBoolean(state, false, time);
		}
		container.addBoolean(enable, false, delay);
	}

	@Override
	protected void onStateChanged(Value value) {
		if (value.booleanValue() && stateValueLast != null && !stateValueLast.booleanValue()) {
			startTimeLast = value.getTime();
		}
	}

}
