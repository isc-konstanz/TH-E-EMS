/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
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
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.RunState;
import org.the.ems.core.RunnableService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;

public abstract class Runnable extends Component implements RunnableService {
	private final static Logger logger = LoggerFactory.getLogger(Runnable.class);

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	protected int runtimeMin = 600000;

	@Configuration(mandatory=false, scale=6000) // Default idletime minimum of 1 minute
	protected int idletimeMin = 60000;

	@Configuration(mandatory=false)
	protected Channel state;

	protected volatile Value stateValueLast = null;
	protected volatile long startTimeLast = 0;
	protected volatile long stopTimeLast = 0;

	protected volatile RunState runState = RunState.DEFAULT;

	@Override
	public RunState getState() {
		return runState;
	}

	public void setState(RunState state) {
		this.runState = state;
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
	public int getIdletime() {
		if (stopTimeLast > 0) {
			return (int) (System.currentTimeMillis() - stopTimeLast);
		}
		return 0;
	}

	@Override
	public int getMinIdletime() {
		return runtimeMin;
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		if (state != null) {
			state.registerValueListener(new StateListener());
		}
		runState = RunState.STANDBY;
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		try {
			stop();
			
		} catch (EnergyManagementException e) {
			logger.warn("Error while stopping {} during deactivation: {}", getId(), e.getMessage());
		}
		if (state != null) {
			state.deregisterValueListeners();
		}
	}

	@Override
	protected void onInterrupt() throws ComponentException {
		super.onInterrupt();
		switch(getState()) {
		case STOPPING:
			if (isStandby()) {
				doStandby();
			}
			break;
		case STARTING:
			if (isRunning()) {
				doRun();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public final void schedule(Schedule schedule)
			throws UnsupportedOperationException, EnergyManagementException {
		
		if (isMaintenance()) {
			throw new MaintenanceException("Unable to schedule component while in maintenance");
		}
		WriteContainer container = new WriteContainer();
		for (Value value : schedule) {
			onSet(container, value);
		}
		write(container);
	}

	void doSchedule(WriteContainer container, Schedule schedule) throws ComponentException {
		long startTimeLast = 0;
		for (int i=0; i<schedule.size(); i++) {
			Value value = schedule.get(i);
			if (value.doubleValue() == 0) {
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

	protected void onSchedule(WriteContainer container, Schedule schedule) 
			throws UnsupportedOperationException, ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public final void set(Value value) 
			throws UnsupportedOperationException, EnergyManagementException {

		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		switch(getState()) {
		case STANDBY:
		case STOPPING:
			if (value.doubleValue() > 0) {
				if (value.getTime() - stopTimeLast < idletimeMin) {
					throw new ComponentException(MessageFormat.format("Unable to start component after interval shorter than {0}mins", 
							idletimeMin/60000));
				}
				doStart(value);
			}
			break;
		case STARTING:
		case RUNNING:
			if (value.doubleValue() == 0) {
				if (value.getTime() - startTimeLast < runtimeMin) {
					throw new ComponentException(MessageFormat.format("Unable to stop component after interval shorter than {0}mins", 
							runtimeMin/60000));
				}
				doStop(value.getTime());
			}
			break;
		default:
			break;
		}
		doSet(value);
	}

	void doSet(Value value) throws EnergyManagementException {
		WriteContainer container = new WriteContainer();
		onSet(container, value);
		write(container);
	}

	protected void onSet(WriteContainer container, Value value)
			throws UnsupportedOperationException, ComponentException {
		// Default implementation to be overridden
		throw new UnsupportedOperationException();
	}

	@Override
	public final void start(Value value) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (!isStartable(value.getTime())) {
			throw new ComponentException("Unable to start component");
		}
		doStart(value);
	}

	void doStart(Value value) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		writeContainer.add(state, new BooleanValue(true, value.getTime()));
		
		setState(RunState.STARTING);
		onStart(writeContainer, value);
		write(writeContainer);
		startTimeLast = value.getTime();
	}

	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public boolean isStartable(long time) {
		switch(getState()) {
		case STANDBY:
		case STOPPING:
            if (time - stopTimeLast >= idletimeMin) {
            	logger.debug("Component is not startable after interval shorter than {}mins", idletimeMin/60000);
    			return true;
            }
		default:
			break;
		}
		return false;
	}

	void doRun() throws ComponentException {
		setState(RunState.RUNNING);
		onRunning();
	}

	protected void onRunning() throws ComponentException {
		// Default implementation to be overridden
	}

	public boolean isRunning() throws ComponentException {
		// Default implementation to be overridden
		switch(getState()) {
		case STARTING:
		case RUNNING:
			return true;
		default:
			return false;
		}
	}

	@Override
	public final void stop(long time) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (!isStoppable(time)) {
			throw new ComponentException("Unable to stop component");
		}
		doStop(time);
	}

	void doStop(long time) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		writeContainer.add(state, new BooleanValue(false, time));
		
		setState(RunState.STOPPING);
		onStop(writeContainer, time);
		write(writeContainer);
		stopTimeLast = time;
	}

	protected void onStop(WriteContainer container, long time) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public boolean isStoppable(long time) {
		switch(getState()) {
		case STANDBY:
		case STOPPING:
            if (time - startTimeLast >= runtimeMin) {
            	logger.debug("Unable to stop component after interval shorter than {}mins", runtimeMin/60000);
    			return true;
            }
		default:
			break;
		}
		return false;
	}

	void doStandby() throws ComponentException {
		setState(RunState.STANDBY);
		onStandby();
	}

	protected void onStandby() throws ComponentException {
		// Default implementation to be overridden
	}

	public boolean isStandby() throws ComponentException {
		// Default implementation to be overridden
		switch(getState()) {
		case STANDBY:
		case STOPPING:
			return true;
		default:
			return false;
		}
	}

	protected void onStateChanged(Value state) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected class StateListener implements ValueListener {

		@Override
		public synchronized void onValueReceived(Value value) {
			if (stateValueLast == null || stateValueLast.booleanValue() != value.booleanValue()) {
				try {
					switch(getState()) {
					case STANDBY:
					case STOPPING:
						if (value.booleanValue()) {
							doStart(getStartValue());
						}
						break;
					case STARTING:
					case RUNNING:
						if (!value.booleanValue()) {
							doStop(System.currentTimeMillis());
						}
						break;
					default:
						break;
					}
					onStateChanged(value);
					
				} catch (EnergyManagementException e) {
					state.setLatestValue(new BooleanValue(!value.booleanValue()));
					logger.warn("Error handling state change: {}", e.getMessage());
				}
			}
			stateValueLast = value;
		}
	}

}
