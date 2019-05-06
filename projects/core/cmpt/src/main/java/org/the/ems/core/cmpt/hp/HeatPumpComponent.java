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
package org.the.ems.core.cmpt.hp;

import java.text.MessageFormat;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.cmpt.circ.Circulation;
import org.the.ems.core.cmpt.circ.CirculationPump;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public abstract class HeatPumpComponent extends ConfiguredComponent implements HeatPumpService {

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000, mandatory=false)
	protected double powerMin = -1;

	@Configuration
	protected double cop;

	protected Circulation circulation;
	protected CirculationPump circulationPump;

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		
		circulation = new Circulation(context, configs);
		circulationPump = new CirculationPump(context, configs, circulation);
	}

	@Override
	public void onResume() throws EnergyManagementException {
		circulationPump.resume();
	}

	@Override
	public void onPause() throws EnergyManagementException {
		circulationPump.pause();
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
		circulation.deactivate();
		circulationPump.deactivate();
	}

	@Override
	public void onSet(ComponentWriteContainer container, Value value) throws ComponentException {
		if (value.doubleValue() == 0) {
			onStop(container, value.getTime());
		}
		else if (value.doubleValue() <= getMaxPower() && value.doubleValue() >= getMinPower()) {
			onStart(container, value);
		}
		throw new ComponentException(MessageFormat.format("Invalid power values passed to set component: {0}", value));
	}

	protected abstract void onStart(ComponentWriteContainer container, Value value) throws ComponentException;

	protected abstract void onStop(ComponentWriteContainer container, Long time) throws ComponentException;

	@Override
	public void start(Value value) throws EnergyManagementException {
		set(value);
	}

	@Override
	public void onStop(Long time) throws EnergyManagementException {
		set(new DoubleValue(0, time));
	}

	@Override
	public double getMaxPower() {
		return powerMax;
	}

	@Override
	public double getMinPower() {
		if (powerMin > 0) {
			return powerMin;
		}
		return getMaxPower();
	}

	@Override
	public Value getCoefficientOfPerformance() throws ComponentException {
		return new DoubleValue(cop);
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
