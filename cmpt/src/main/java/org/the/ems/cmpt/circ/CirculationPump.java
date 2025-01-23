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
package org.the.ems.cmpt.circ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.Circulation.CirculationCallbacks;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class CirculationPump extends Component implements CirculationCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(CirculationPump.class);

	private final static String STATE = "state";

	private final Circulation circulation;

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	private int runtimeMin = 600000;

	@Configuration(mandatory=false)
	private double flowTempDeltaMin = 0;

	@Configuration
	private Channel state;

	private Value stateValueLast = new BooleanValue(false);

	private volatile long startTimeLast = 0;

	private volatile boolean running = false;

	public CirculationPump(Circulation circulation) {
		super(Circulation.SECTION);
		this.circulation = circulation;
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		if (!isEnabled()) {
			return;
		}
		circulation.register(this);
		state.registerValueListener(new CirculationPumpStateListener());
		
		running = true;
		logger.debug("Activated TH-E EMS CirculationPump");
	}

	public void resume() {
		running = true;
	}

	public void pause() {
		running = false;
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		if (isEnabled()) {
			circulation.deregister();
		}
		running = false;
	}

	@Override
	public boolean isEnabled() {
		if (!getConfigurations().containsKey(Circulation.SECTION, STATE)) {
			return false;
		}
		return super.isEnabled();
	}

	public boolean isRunning() {
		return isEnabled() && running;
	}

	public boolean hasRunMinimum() {
		return System.currentTimeMillis() - startTimeLast >= runtimeMin;
	}

	public void start() throws EnergyManagementException {
		if (!stateValueLast.booleanValue()) {
			state.write(new BooleanValue(true));
		}
	}

	public void stop() throws EnergyManagementException {
		if (stateValueLast.booleanValue()) {
			state.write(new BooleanValue(false));
		}
	}

	@Override
	public void onTemperatureDeltaUpdated(Value delta) {
		if (flowTempDeltaMin > 0 && flowTempDeltaMin >= delta.doubleValue() && isRunning() && hasRunMinimum()) {
			try {
				stop();
				
			} catch (EnergyManagementException e) {
				logger.warn("Error stopping circulation pump: {}", e.getMessage());
			}
		}
	}

	private class CirculationPumpStateListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			boolean state = value.booleanValue();
			if (state && !stateValueLast.booleanValue()) {
				startTimeLast = value.getEpochMillis();
			}
			stateValueLast = value;
		}
	}

}
