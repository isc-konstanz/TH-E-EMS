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
package org.the.ems.cmpt;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.Circulation;
import org.the.ems.cmpt.circ.CirculationPump;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.GeneratorState;
import org.the.ems.core.HeatingService;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;

public abstract class Heating extends Component implements HeatingService {
	private final static Logger logger = LoggerFactory.getLogger(Heating.class);

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	protected int runtimeMin = 600000;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000, mandatory=false)
	protected double powerMin = -1;

	@Configuration
	protected ChannelListener state;

	protected Circulation circulation;
	protected CirculationPump circulationPump;

	protected volatile GeneratorState generatorState = GeneratorState.STANDBY;

	protected volatile Value stateValueLast = null;
	protected volatile long startTimeLast = 0;

	@Override
	public GeneratorState getState() {
		return generatorState;
	}

	public void setState(GeneratorState state) {
		this.generatorState = state;
	}

	@Override
	public int getRuntime() {
		if (startTimeLast > 0) {
			return (int) (System.currentTimeMillis() - startTimeLast);
		}
		return 0;
	}

	@Override
	public int getMinRuntime() {
		return runtimeMin;
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
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		state.registerValueListener(new StateListener());
		circulation = new Circulation().activate(content).configure(configs);
		circulationPump = new CirculationPump(circulation);
	}

	@Override
	public void onResume() throws ComponentException {
		circulationPump.resume();
	}

	@Override
	public void onPause() throws ComponentException {
		circulationPump.pause();
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		state.deregister();
		circulation.deactivate();
		circulationPump.deactivate();
	}

	@Override
	public void doSchedule(WriteContainer container, Schedule schedule) throws ComponentException {
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
	public void doSet(WriteContainer container, Value value) throws EnergyManagementException {
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
	public final void start(Value value) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (value.doubleValue() <= 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		switch(getState()) {
		case STANDBY:
		case STOPPING:
			setState(GeneratorState.STARTING);
			
			WriteContainer container = new WriteContainer();
			onStart(container, value);
			doWrite(container);
			
			startTimeLast = value.getTime();
			break;
		default:
			break;
		}
	}

	protected abstract void onStart(WriteContainer container, Value value) throws ComponentException;

	@Override
	public final void stop(long time) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (time - startTimeLast < runtimeMin) {
			throw new ComponentException(MessageFormat.format("Unable to stop component after interval shorter than {0}mins", 
					runtimeMin/60000));
		}
		switch(getState()) {
		case RUNNING:
		case STARTING:
			setState(GeneratorState.STOPPING);
			
			WriteContainer container = new WriteContainer();
			onStop(container, time);
			doWrite(container);
			
			break;
		default:
			break;
		}
	}

	protected abstract void onStop(WriteContainer container, long time) throws ComponentException;

	protected void onStateChanged(Value value) {
	}

	protected class StateListener implements ValueListener {

		@Override
		public void onValueReceived(Value state) {
			if (stateValueLast == null || stateValueLast.booleanValue() != state.booleanValue()) {
				if (state.booleanValue()) {
					if (!circulationPump.isDisabled()) {
						circulationPump.start();
					}
					startTimeLast = state.getTime();
					
					// TODO: Verify if the generator really has started
					setState(GeneratorState.RUNNING);
				}
				else {
					// TODO: Verify if the generator really has stopped
					setState(GeneratorState.STANDBY);
				}
				onStateChanged(state);
			}
			stateValueLast = state;
		}
	}

}
