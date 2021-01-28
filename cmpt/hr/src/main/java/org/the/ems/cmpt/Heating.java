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
package org.the.ems.cmpt;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.Circulation;
import org.the.ems.cmpt.circ.CirculationPump;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.RunState;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;

public abstract class Heating extends Runnable implements HeatingService {
	private final static Logger logger = LoggerFactory.getLogger(Heating.class);

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000, mandatory=false)
	protected double powerMin = -1;

	protected Circulation circulation;
	protected CirculationPump circulationPump;

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
        if (powerMin > 0) {
            return powerMin;
        }
        return getMaxPower();
    }

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		circulation = new Circulation().activate(content).configure(configs);
		circulationPump = new CirculationPump(circulation).activate(content).configure(configs);
	}

	@Override
	protected void onResume() throws ComponentException {
		circulationPump.resume();
	}

	@Override
	protected void onPause() throws ComponentException {
		circulationPump.pause();
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		circulation.deactivate();
		circulationPump.deactivate();
	}

	@Override
	protected void doSchedule(WriteContainer container, Schedule schedule) throws ComponentException {
		long startTimeLast = 0;
		for (int i=0; i<schedule.size(); i++) {
			Value value = schedule.get(i);
			if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
				throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
			}
			else if (value.doubleValue() == 0) {
				if (value.getTime() - startTimeLast < runtimeMin) {
					logger.debug("Unable to stop component after interval shorter than {}mins", runtimeMin/60000);
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

	@Override
	protected void doSet(Value value) throws EnergyManagementException {
		if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		WriteContainer container = new WriteContainer();
		onSet(container, value);
		doWrite(container);
	}

	@Override
	protected void doStart(Value value) throws EnergyManagementException {
		if (value.doubleValue() <= 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		WriteContainer writeContainer = new WriteContainer();
		writeContainer.add(state, new BooleanValue(true, value.getTime()));
		
		setState(RunState.STARTING);
		onStart(writeContainer, value);
		doWrite(writeContainer);
		startTimeLast = value.getTime();
	}

	@Override
	protected void onStateChanged(Value state) throws EnergyManagementException {
		if (state.booleanValue()) {
			if (circulationPump.isEnabled()) {
				circulationPump.start();
			}
		}
	}

}
