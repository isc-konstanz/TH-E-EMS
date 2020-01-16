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
package org.the.ems.cmpt.circ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.Circulation.CirculationCallbacks;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.config.Configurable;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class CirculationPump extends Configurable implements CirculationCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(CirculationPump.class);

	private final static String SECTION = "Circulation";

	private final static String STATE = "state";

	private final Circulation circulation;

	@Configuration(mandatory=false, scale=60000) // Default runtime minimum of 10 minutes
	private int runtimeMin = 600000;

	@Configuration(mandatory=false)
	private double flowTempDeltaMin = 0.5;

	@Configuration
	private ChannelListener state;

	private Value stateValueLast = new BooleanValue(false);

	private volatile long startTimeLast = 0;

	private volatile boolean running = false;

	public CirculationPump(Circulation circulation) {
		this.circulation = circulation;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CirculationPump activate(ContentManagementService content) throws ComponentException {
		super.activate(content);
		return setConfiguredSection(SECTION);
	}

	@Override
	@SuppressWarnings("unchecked")
	public CirculationPump configure(Configurations configs) throws ConfigurationException {
		super.configure(configs);
		if (!configs.contains(SECTION, STATE)) {
			return this;
		}
		if (!isDisabled()) {
			circulation.register(this);
			state.registerValueListener(new CirculationPumpStateListener());
			
			running = true;
			logger.debug("Activated TH-E EMS CirculationPump");
		}
		return this;
	}

	public void resume() {
		running = true;
	}

	public void pause() {
		running = false;
	}

	public void deactivate() {
		if (!isDisabled()) {
			circulation.deregister();
			state.deregister();
		}
		running = false;
	}

	public boolean isRunning() {
		return !isDisabled() && running;
	}

	public boolean hasRunMinimum() {
		return System.currentTimeMillis() - startTimeLast >= runtimeMin;
	}

	public void start() {
		if (!stateValueLast.booleanValue()) {
			state.write(new BooleanValue(true));
		}
	}

	public void stop() {
		if (stateValueLast.booleanValue()) {
			state.write(new BooleanValue(false));
		}
	}

	@Override
	public void onTemperatureDeltaUpdated(Value delta) {
		if (isRunning() && hasRunMinimum() && delta.doubleValue() <= flowTempDeltaMin) {
			stop();
		}
	}

	private class CirculationPumpStateListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			boolean state = value.booleanValue();
			if (state && !stateValueLast.booleanValue()) {
				startTimeLast = value.getTime();
			}
			stateValueLast = value;
		}
	}

}
