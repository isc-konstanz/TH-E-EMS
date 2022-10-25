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
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentBusyException;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.RunState;
import org.the.ems.core.RunStateListener;
import org.the.ems.core.RunnableService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;

public abstract class Runnable extends Component implements RunnableService {
	private final static Logger logger = LoggerFactory.getLogger(Runnable.class);

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	private int runtimeMin = 600000;

	@Configuration(mandatory=false, scale=6000) // Default idletime minimum of 1 minute
	private int idletimeMin = 60000;

	@Configuration(mandatory = false, scale = 1000)
	private int runActionWritePause = 10000;

	private volatile long runActionWriteTime = Long.MIN_VALUE;

	@Configuration(mandatory = false, scale = 1000)
	private int runStateValidationInterval = 10000;

	private volatile long runStateValidationTime = 0;

	private volatile RunState runState = RunState.DEFAULT;

	@Configuration(mandatory=false, value="state_writable")
	protected boolean stateIsWritable = true;

	@Configuration(mandatory=false)
	protected Channel state;

	private volatile Value stateValue = null;
	private volatile long startTimeLast = Long.MIN_VALUE;
	private volatile long stopTimeLast = Long.MIN_VALUE;

	private final List<RunStateListener> runStateListeners = new LinkedList<RunStateListener>();

	@Override
	public RunState getState() {
		return runState;
	}

	@Override
	public RunState getState(RunStateListener listener) throws ComponentException, InvalidValueException {
		registerStateListener(listener);
		return getState();
	}

	@Override
	public void registerStateListener(RunStateListener listener) throws ComponentException {
		synchronized (runStateListeners) {
			if (!runStateListeners.contains(listener)) {
				runStateListeners.add(listener);
			}
		}
	}

	@Override
	public void deregisterStateListener(RunStateListener listener) throws ComponentException {
		synchronized (runStateListeners) {
			if (runStateListeners.contains(listener)) {
				runStateListeners.remove(listener);
			}
		}
	}

	protected void deregisterStateListeners() {
		synchronized (runStateListeners) {
			runStateListeners.clear();
		}
	}

	public void setState(RunState state) {
		if (runState != state) {
			synchronized (runStateListeners) {
				for (RunStateListener stateListener : runStateListeners) {
					try {
						stateListener.onStateChanged(state);
						
					} catch (EnergyManagementException e) {
						logger.warn("Error notifying of state change: {}", e.getMessage());
					}
				}
			}
		}
		this.runState = state;
	}

	@Override
	public Value getStateValue() throws ComponentException, InvalidValueException {
		return state.getLatestValue();
	}

	@Override
	public Value getStateValue(ValueListener listener) throws ComponentException, InvalidValueException {
		return state.getLatestValue(listener);
	}

	@Override
	public void registerStateValueListener(ValueListener listener) throws ComponentException {
		state.registerValueListener(listener);
	}

	@Override
	public void deregisterStateValueListener(ValueListener listener) throws ComponentException {
		state.deregisterValueListener(listener);
	}

	@Override
	public int getRuntime() {
		return getRuntime(System.currentTimeMillis());
	}

	public int getRuntime(long timestamp) {
		if (startTimeLast > 0) {
			return (int) (timestamp - startTimeLast);
		}
		return getMinRuntime() + 1;
	}

	@Override
	public int getMinRuntime() {
		return runtimeMin;
	}

	@Override
	public int getIdletime() {
		return getIdletime(System.currentTimeMillis());
	}

	public int getIdletime(long timestamp) {
		if (stopTimeLast > 0) {
			return (int) (timestamp - stopTimeLast);
		}
		return getMinIdletime() + 1;
	}

	@Override
	public int getMinIdletime() {
		return idletimeMin;
	}

	@Override
	public StartSettings getStartSettings(long timestamp) throws ComponentException, InvalidValueException {
		Value value = getStartValue(timestamp);
		return new ValueSettings(value);
	}

	@Override
	public StopSettings getStopSettings(long timestamp) throws ComponentException, InvalidValueException {
		Value value = getStopValue(timestamp);
		return new ValueSettings(value);
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
	}

	@Override
	protected void onInterrupt() throws ComponentException {
		super.onInterrupt();
		long timestamp = System.currentTimeMillis();
		if (timestamp - runStateValidationTime > runStateValidationInterval) {
			switch(getState()) {
			case STARTING:
			case STANDBY:
				if (isRunning()) {
					doRunning();
				}
				break;
			case STOPPING:
			case RUNNING:
				if (isStandby()) {
					doStandby();
				}
				break;
			default:
				break;
			}
			runStateValidationTime = timestamp;
		}
	}

	public boolean isReady() {
		if (!isEnabled() || isMaintenance()) {
			return false;
		}
		return isReady(System.currentTimeMillis());
	}

	protected boolean isReady(long timestamp) {
		if (runActionWriteTime > 0 && timestamp - runActionWriteTime < runActionWritePause) {
			return false;
		}
		return true;
	}

	@Override
	public final void schedule(Schedule schedule)
			throws UnsupportedOperationException, EnergyManagementException {
		
		if (isMaintenance()) {
			throw new MaintenanceException("Unable to schedule component while in maintenance");
		}
		doSchedule(schedule);
	}

	void doSchedule(Schedule schedule) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		
		long startTimeLast = 0;
		for (int i=0; i<schedule.size(); i++) {
			Value value = schedule.get(i);
			if (value.doubleValue() == getStopValue(value.getEpochMillis()).doubleValue()) {
				if (value.getEpochMillis() - startTimeLast < runtimeMin) {
					logger.debug("Unable to stop component after interval shorter than {}mins", runtimeMin/60000);
					continue;
				}
				onStop(writeContainer, value);
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() == 0) {
				startTimeLast = value.getEpochMillis();
				onStart(writeContainer, value);
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() != value.doubleValue()) {
				onSet(writeContainer, value);
			}
		}
		onSchedule(writeContainer, schedule);
        if (writeContainer.size() > 0) {
    		logger.debug("Scheduling {} {} values", getType().getFullName(), getId());
    		if (logger.isDebugEnabled()) {
    			for (Entry<Channel, ValueList> channel : writeContainer.entrySet()) {
    				for (Value value : channel.getValue()) {
    					logger.debug("Writing value to channel \"{}\": {}", channel.getKey().getId(), value);
    				}
    			}
    		}
			runActionWriteTime = writeContainer.values()
					.stream().skip(writeContainer.size() - 1)
					.findFirst().get().getLast().getEpochMillis();
        }
		write(writeContainer);
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
		Value stopValue = getStopValue(value.getEpochMillis());
		switch(getState()) {
		case STANDBY:
		case STOPPING:
			if (value.doubleValue() > stopValue.doubleValue()) {
				if (getIdletime(value.getEpochMillis()) < idletimeMin) {
					throw new ComponentException(MessageFormat.format("Unable to start component after interval shorter than {0}mins", 
							idletimeMin/60000));
				}
				doStart(new ValueSettings(value));
				return;
			}
			break;
		case STARTING:
		case RUNNING:
			if (value.doubleValue() == stopValue.doubleValue()) {
				if (getRuntime(value.getEpochMillis()) < runtimeMin) {
					throw new ComponentException(MessageFormat.format("Unable to stop component after interval shorter than {0}mins", 
							runtimeMin/60000));
				}
				doStop(new ValueSettings(value));
				return;
			}
			break;
		default:
			break;
		}
		doSet(value);
	}

	void doSet(Value setpoint) throws EnergyManagementException {
		if (!isReady(setpoint.getEpochMillis())) {
			throw new ComponentBusyException(String.format("{} component {} already written to {}s ago", 
					getType().getFullName(), getId(), (System.currentTimeMillis() - runActionWriteTime)/1000));
		}
		WriteContainer writeContainer = new WriteContainer();
		onSet(writeContainer, setpoint);
        if (writeContainer.size() > 0) {
    		logger.debug("Writing {} {} setpoint: {}", getType().getFullName(), getId(), setpoint);
    		if (logger.isDebugEnabled()) {
    			for (Entry<Channel, ValueList> channel : writeContainer.entrySet()) {
    				for (Value value : channel.getValue()) {
    					logger.debug("Writing value to channel \"{}\": {}", channel.getKey().getId(), value);
    				}
    			}
    		}
			runActionWriteTime = writeContainer.values()
					.stream().skip(writeContainer.size() - 1)
					.findFirst().get().getLast().getEpochMillis();
        }
		write(writeContainer);
	}

	protected void onSet(WriteContainer container, Value value)
			throws UnsupportedOperationException, ComponentException {
		// Default implementation to be overridden
		throw new UnsupportedOperationException();
	}

	@Override
	public final void start(long timestamp) throws EnergyManagementException {
		start(getStartSettings(timestamp));
	}

	@Override
	public final void start(StartSettings settings) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (!isStartable(settings.getEpochMillis()) && !settings.isEnforced()) {
			throw new ComponentException("Unable to start component");
		}
		doStart(settings);
	}

	void doStart(StartSettings settings) throws EnergyManagementException {
		if (!isReady(settings.getEpochMillis())) {
			throw new ComponentBusyException(String.format("{} component {} already written to {}s ago", 
					getType().getFullName(), getId(), (System.currentTimeMillis() - runActionWriteTime)/1000));
		}
		WriteContainer writeContainer = new WriteContainer();
		if (stateIsWritable && state != null) {
			writeContainer.add(state, new BooleanValue(true, settings.getEpochMillis()));
		}
		doStart(writeContainer, settings);
        if (writeContainer.size() > 0) {
    		logger.debug("Starting {} {}", getType().getFullName(), getId());
    		if (logger.isDebugEnabled()) {
    			for (Entry<Channel, ValueList> channel : writeContainer.entrySet()) {
    				for (Value value : channel.getValue()) {
    					logger.debug("Writing value to channel \"{}\": {}", channel.getKey().getId(), value);
    				}
    			}
    		}
			runActionWriteTime = writeContainer.values()
					.stream().skip(writeContainer.size() - 1)
					.findFirst().get().getLast().getEpochMillis();
        }
		write(writeContainer);

		// Set latest start time and state when writing was successful
		startTimeLast = settings.getEpochMillis();
		setState(RunState.STARTING);
	}

	void doStart(WriteContainer container, StartSettings settings) throws EnergyManagementException {
		if (settings instanceof ValueSettings) {
			doStart(container, (ValueSettings) settings);
		}
		onStart(container, settings);
	}

	protected void onStart(WriteContainer container, StartSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	void doStart(WriteContainer container, ValueSettings settings) throws EnergyManagementException {
		onStart(container, settings);
		onStart(container, settings.getValue());
	}

	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public boolean isStartable(long time) {
		switch(getState()) {
		case STANDBY:
		case STOPPING:
            if (getIdletime(time) <= idletimeMin) {
            	logger.debug("Component is not startable after interval shorter than {}mins", idletimeMin/60000);
    			return false;
            }
		default:
			break;
		}
		return isReady(time);
	}

	void doRunning() throws ComponentException {
		onRunning();
		setState(RunState.RUNNING);
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
	public final void stop(long timestamp) throws EnergyManagementException {
		stop(getStopSettings(timestamp));
	}

	@Override
	public final void stop(StopSettings settings) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (!isStoppable(settings.getEpochMillis()) && !settings.isEnforced()) {
			throw new ComponentException("Unable to stop component");
		}
		doStop(settings);
	}

	void doStop(StopSettings settings) throws EnergyManagementException {
		if (!isReady(settings.getEpochMillis())) {
			throw new ComponentBusyException(String.format("{} component {} already written to {}s ago", 
					getType().getFullName(), getId(), (System.currentTimeMillis() - runActionWriteTime)/1000));
		}
		WriteContainer writeContainer = new WriteContainer();
		if (stateIsWritable && state != null) {
			writeContainer.add(state, new BooleanValue(false, settings.getEpochMillis()));
		}
		doStop(writeContainer, settings);
        if (writeContainer.size() > 0) {
			logger.debug("Stopping {} {}", getType().getFullName(), getId());
			if (logger.isDebugEnabled()) {
				for (Entry<Channel, ValueList> channel : writeContainer.entrySet()) {
					for (Value value : channel.getValue()) {
						logger.debug("Writing value to channel \"{}\": {}", channel.getKey().getId(), value);
					}
				}
			}
			runActionWriteTime = writeContainer.values()
					.stream().skip(writeContainer.size() - 1)
					.findFirst().get().getLast().getEpochMillis();
        }
		write(writeContainer);

		// Set latest stop time and state when writing was successful
		stopTimeLast = settings.getEpochMillis();
		setState(RunState.STOPPING);
	}

	void doStop(WriteContainer container, StopSettings settings) throws EnergyManagementException {
		if (settings instanceof ValueSettings) {
			doStop(container, (ValueSettings) settings);
		}
		onStop(container, settings);
	}

	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	void doStop(WriteContainer container, ValueSettings settings) throws EnergyManagementException {
		onStop(container, settings);
		onStop(container, settings.getValue());
	}

	protected void onStop(WriteContainer container, ValueSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onStop(WriteContainer container, Value value) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public boolean isStoppable(long time) {
		switch(getState()) {
		case STARTING:
		case RUNNING:
            if (getRuntime(time) <= runtimeMin) {
            	logger.debug("Unable to stop component after interval shorter than {}mins", runtimeMin/60000);
    			return false;
            }
		default:
			break;
		}
		return isReady(time);
	}

	void doStandby() throws ComponentException {
		onStandby();
		setState(RunState.STANDBY);
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

	protected void onStateChanged(RunState state) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected void onStateChanged(Value state) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected class StateListener implements ValueListener {

		@Override
		public synchronized void onValueReceived(Value value) {
			if (stateValue == null || !stateValue.equals(value)) {
				try {
					switch(getState()) {
					case STARTING:
					case STANDBY:
						if (value.booleanValue()) {
							if (stateIsWritable) {
								doStart(getStartSettings(System.currentTimeMillis()));
							}
							else if (isRunning()) {
								onRunning();
								setState(RunState.RUNNING);
							}
						}
						break;
					case STOPPING:
					case RUNNING:
						if (!value.booleanValue()) {
							if (stateIsWritable) {
								doStop(getStopSettings(System.currentTimeMillis()));
							}
							else if (isStandby()) {
								onStandby();
								setState(RunState.STANDBY);
							}
						}
						break;
					default:
						break;
					}
				} catch (EnergyManagementException e) {
					if (stateIsWritable) {
						state.setLatestValue(new BooleanValue(!value.booleanValue()));
					}
					logger.warn("Error handling state change: {}", e.getMessage());
				}
				try {
					onStateChanged(value);
					
					synchronized (runStateListeners) {
						for (RunStateListener stateListener : runStateListeners) {
							stateListener.onStateChanged(value);
						}
					}
				} catch (EnergyManagementException e) {
					logger.warn("Error notifying of state change: {}", e.getMessage());
				}
			}
			stateValue = value;
		}
	}

}
