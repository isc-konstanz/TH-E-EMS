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
package org.the.ems.core.cmpt;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.GeneratorService;
import org.the.ems.core.GeneratorState;
import org.the.ems.core.cmpt.circ.Circulation;
import org.the.ems.core.cmpt.circ.CirculationPump;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;

public abstract class GeneratorComponent extends ConfiguredComponent implements GeneratorService {
	private final static Logger logger = LoggerFactory.getLogger(GeneratorComponent.class);

	@Configuration(mandatory=false, scale=60000) // Default interval minimum of 10 minutes
	protected int intervalMin = 600000;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000, mandatory=false)
	protected double powerMin = -1;

	protected Circulation circulation;
	protected CirculationPump circulationPump;

	protected volatile GeneratorState generatorState = GeneratorState.STANDBY;

	protected volatile long startTimeLast = 0;

	@Override
	public GeneratorState getState() {
		return generatorState;
	}

	public void setState(GeneratorState state) {
		this.generatorState = state;
	}

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
	public void onSchedule(WriteContainer container, Schedule schedule) throws ComponentException {
		long startTimeLast = 0;
		for (int i=0; i<schedule.size(); i++) {
			Value value = schedule.get(i);
			if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
				throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
			}
			else if (value.doubleValue() == 0) {
				if (value.getTime() - startTimeLast < intervalMin) {
					logger.debug("Unable to stop component after interval shorter than {}mins", intervalMin/60000);
					continue;
				}
				onStop(container, value.getTime());
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() == 0) {
				startTimeLast = value.getTime();
				onStart(container, value);
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() != value.doubleValue()) {
				onSet(container, value);
			}
		}
	}

	protected abstract void onStart(WriteContainer container, Value value) throws ComponentException;

	protected abstract void onStop(WriteContainer container, Long time) throws ComponentException;

	@Override
	public void set(Value value) throws EnergyManagementException, UnsupportedOperationException {
		WriteContainer container = new WriteContainer();
		if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		else if (value.doubleValue() == 0) {
			onStop(container, value.getTime());
		}
		else {
			onSet(container, value);
		}
		doWrite(container);
	}

	@Override
	public void start(Value value) throws EnergyManagementException {
		if (value.doubleValue() <= 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		setState(GeneratorState.STARTING);
		startTimeLast = value.getTime();
		
		WriteContainer container = new WriteContainer();
		onStart(container, value);
		doWrite(container);
	}

	@Override
	public void stop(Long time) throws EnergyManagementException {
		if (time - startTimeLast < intervalMin && !isMaintenance()) {
			throw new ComponentException(MessageFormat.format("Unable to stop component after interval shorter than {0}mins", 
					intervalMin/60000));
		}
		setState(GeneratorState.STOPPING);
		
		WriteContainer container = new WriteContainer();
		onStop(container, time);
		doWrite(container);
	}

	public double getMaxPower() {
		return powerMax;
	}

	public double getMinPower() {
		if (powerMin > 0) {
			return powerMin;
		}
		return getMaxPower();
	}

}
