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
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingState;
import org.the.ems.core.HeatingService;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;

public abstract class Heating extends Component implements HeatingService {
	private final static Logger logger = LoggerFactory.getLogger(Heating.class);

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	protected int runtimeMin = 600000;

	@Configuration(mandatory=false, scale=6000) // Default idletime minimum of 1 minute
	protected int idletimeMin = 60000;

	@Configuration(scale=1000)
	protected double powerMax;

	@Configuration(scale=1000, mandatory=false)
	protected double powerMin = -1;

	@Configuration
	protected ChannelListener state;

	protected Circulation circulation;
	protected CirculationPump circulationPump;

	protected volatile HeatingState heatingState = HeatingState.STANDBY;

	protected volatile Value stateValueLast = null;
	protected volatile long startTimeLast = 0;
	protected volatile long stopTimeLast = 0;

	@Override
	public HeatingState getState() {
		return heatingState;
	}

	public void setState(HeatingState state) {
		this.heatingState = state;
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

	protected double getStartPower() {
		return getMaxPower();
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		state.registerValueListener(new StateListener());
		circulation = new Circulation().activate(content).configure(configs);
		circulationPump = new CirculationPump(circulation).activate(content).configure(configs);
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
		try {
			stop();
			
		} catch (EnergyManagementException e) {
			logger.warn("Error while stopping {} during deactivation: {}", id, e.getMessage());
		}
		state.deregister();
		circulation.deactivate();
		circulationPump.deactivate();
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
		doWrite(container);
	}

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
			else {
				doSet(value);
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
		}
	}

	protected void doSet(Value value) throws EnergyManagementException {
		if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		WriteContainer container = new WriteContainer();
		onSet(container, value);
		doWrite(container);
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
		switch(getState()) {
		case STANDBY:
		case STOPPING:
			if (value.getTime() - stopTimeLast < idletimeMin) {
				throw new ComponentException(MessageFormat.format("Unable to start component after interval shorter than {0}mins", 
						idletimeMin/60000));
			}
			doStart(value);
			break;
		default:
			break;
		}
	}

	protected void doStart(Value value) throws EnergyManagementException {
		if (value.doubleValue() <= 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		WriteContainer writeContainer = new WriteContainer();
		writeContainer.add(state, new BooleanValue(true, value.getTime()));
		
		setState(HeatingState.STARTING);
		onStart(writeContainer, value);
		doWrite(writeContainer);
		startTimeLast = value.getTime();
	}

	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public final void stop(long time) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		switch(getState()) {
		case STARTING:
		case RUNNING:
			if (time - startTimeLast < runtimeMin) {
				throw new ComponentException(MessageFormat.format("Unable to stop component after interval shorter than {0}mins", 
						runtimeMin/60000));
			}
			doStop(time);
			break;
		default:
			break;
		}
	}

	protected void doStop(long time) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		writeContainer.add(state, new BooleanValue(false, time));
		
		setState(HeatingState.STOPPING);
		onStop(writeContainer, time);
		doWrite(writeContainer);
		stopTimeLast = time;
	}

	protected void doWrite(WriteContainer container) throws EnergyManagementException {
		if (container.size() < 1) {
			return;
		}
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

	protected void onStop(WriteContainer container, long time) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onStateChanged(Value state) throws EnergyManagementException {
		if (state.booleanValue()) {
			if (!circulationPump.isDisabled()) {
				circulationPump.start();
			}
		}
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
							doStart(new DoubleValue(getStartPower()));
						}
						break;
					case STARTING:
					case RUNNING:
						if (!value.booleanValue()) {
							doStop(System.currentTimeMillis());
						}
						break;
					}
					onStateChanged(value);
					
					if (value.booleanValue()) {
						startTimeLast = value.getTime();
						
						// TODO: Verify if the heating really has started
						setState(HeatingState.RUNNING);
					}
					else {
						stopTimeLast = value.getTime();
						
						// TODO: Verify if the heating really has stopped
						setState(HeatingState.STANDBY);
					}
				} catch (EnergyManagementException e) {
					state.setLatestValue(new BooleanValue(!value.booleanValue()));
					logger.warn("Error handling state change: {}", e.getMessage());
				}
			}
			stateValueLast = value;
		}
	}

}
