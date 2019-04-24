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
package org.the.ems.core.cmpt.inv.ext;

import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;


public class ExternalSolar {

	private Channel virtualPower;
	private ChannelListener powerListener;
	private ChannelListener solarListener;
	private Value solarPower = DoubleValue.emptyValue();
	private Value solarEnergy = null;

	private volatile boolean enabled = false;

	public ExternalSolar(ContentManagementService control, Configurations configs) throws ConfigurationException {
		try {
			if (configs.hasSection(ExternalSolarConfig.class)) {
				ExternalSolarConfig config = configs.getSection(ExternalSolarConfig.class);
				if (!config.isEnabled()) {
					return;
				}
				
				virtualPower = control.getChannel(config.getVirtualPower());
				powerListener = registerActivePowerListener(control.getChannel(config.getActualPower()));
				if (config.hasSolarPower()) {
					solarListener = registerSolarPowerListener(control.getChannel(config.getSolarPower()));
				}
				else {
					solarListener = registerSolarEnergyListener(control.getChannel(config.getSolarEnergy()));
				}
				enabled = true;
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid external configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerActivePowerListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (enabled) {
					DoubleValue virtualValue = new DoubleValue(value.doubleValue() - solarPower.doubleValue(), value.getTime());
					virtualPower.setLatestValue(virtualValue);
				}
			}
		};
		return listener;
	}

	private ChannelListener registerSolarPowerListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				solarPower = value;
			}
		};
		return listener;
	}

	private ChannelListener registerSolarEnergyListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (solarEnergy != null) {
					double hours = ((double) value.getTime() - (double) solarEnergy.getTime())/3600000;
					if (hours > 0) {
						solarPower = new DoubleValue((value.doubleValue() - solarEnergy.doubleValue())*1000/hours, value.getTime());
					}
				}
				solarEnergy = value;
			}
		};
		return listener;
	}

	public void deactivate() {
		powerListener.deregister();
		solarListener.deregister();
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

	public Value getSolar() {
		return solarPower;
	}
}
