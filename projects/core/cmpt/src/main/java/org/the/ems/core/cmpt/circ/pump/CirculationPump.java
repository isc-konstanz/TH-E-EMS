/* 
 * Copyright 2016-18 ISC Konstanz
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
package org.the.ems.core.cmpt.circ.pump;

import org.the.ems.core.ContentManagementService;
import org.the.ems.core.cmpt.circ.Circulation;
import org.the.ems.core.cmpt.circ.Circulation.CirculationCallbacks;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

public class CirculationPump implements CirculationCallbacks {

	private Circulation circulation;

	private double deltaTemperatureMin;

	private Channel state;
	private ChannelListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private int intervalMin;

	private volatile boolean enabled = false;

	public CirculationPump(ContentManagementService manager, Circulation circulation, Configurations configs) throws ConfigurationException {
		try {
			if (configs.hasSection(CirculationPumpConfig.class)) {
				CirculationPumpConfig config = configs.getSection(CirculationPumpConfig.class);
				if (!config.isEnabled()) {
					return;
				}
				this.deltaTemperatureMin = config.getDeltaTemperatureMin();

				circulation.register(this);
				this.circulation = circulation;

				this.state = manager.getChannel(config.getState());
				this.stateListener = registerStateListener(state);
				this.intervalMin = config.getIntervalMin();

				this.enabled = true;
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid circulation pump configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				boolean state = value.booleanValue();
				if (state && !stateValueLast.booleanValue()) {
					startTimeLast = value.getTime();
				}
				stateValueLast = value;
			}
		};
		return listener;
	}

	public void deactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		circulation.deregister();
	}

	public void disable() {
		setEnabled(false);
	}

	public void enable() {
		setEnabled(true);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isRunInterval() {
		return System.currentTimeMillis() - startTimeLast >= intervalMin;
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
		if (isEnabled() && isRunInterval() && delta.doubleValue() <= deltaTemperatureMin) {
			stop();
		}
	}

}
