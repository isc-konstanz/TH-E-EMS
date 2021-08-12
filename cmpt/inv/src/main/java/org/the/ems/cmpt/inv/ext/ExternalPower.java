/* 
 * Copyright 2016-21 ISC Konstanz
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
package org.the.ems.cmpt.inv.ext;

import org.the.ems.cmpt.inv.InverterCallbacks;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configurable;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class ExternalPower extends Configurable implements ValueListener {

	private final static String SECTION = "External";

	private volatile InverterCallbacks callbacks = null;

	@Configuration
	private Channel virtualPower;

	@Configuration
	private ChannelListener activePower;

	@Configuration
	private ChannelListener solarPower;

	@Configuration(mandatory=false)
	private ChannelListener solarEnergy;
	private Value solarEnergyLast = null;

	private volatile boolean running = false;

	@Override
	@SuppressWarnings("unchecked")
	public ExternalPower activate(ContentManagementService content) throws ComponentException {
		super.activate(content);
		return setConfiguredSection(SECTION);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ExternalPower configure(Configurations configs) throws ConfigurationException {
		if (configs.isEnabled(SECTION)) {
			super.configure(configs);
			
			activePower.registerValueListener(new ActivePowerListener());
			solarPower.registerValueListener(this);
			if (solarEnergy != null) {
				solarEnergy.registerValueListener(new SolarEnergyListener());
			}
			running = true;
		}
		return this;
	}

	public ExternalPower register(InverterCallbacks callbacks) {
		this.callbacks = callbacks;
		return this;
	}

	public void resume() {
		running = true;
	}

	public void pause() {
		running = false;
	}

	public void deregister() {
		this.callbacks = null;
	}

	public void deactivate() {
		if (isEnabled()) {
			activePower.deregister();
			solarPower.deregister();
			if (solarEnergy != null) {
				solarEnergy.deregister();
			}
		}
		running = false;
	}

	public boolean isRunning() {
		return isEnabled() && running;
	}

	public Value getSolar() {
		Value value;
		try {
			value = solarPower.getLatestValue();
			
		} catch (InvalidValueException e) {
			value = DoubleValue.emptyValue();
		}
		return value;
	}

	@Override
	public void onValueReceived(Value value) {
		if (callbacks != null) {
			callbacks.onSetpointUpdate();
		}
	}

	private class ActivePowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (isRunning()) {
				DoubleValue virtualValue = new DoubleValue(value.doubleValue() - getSolar().doubleValue(), value.getTime());
				virtualPower.setLatestValue(virtualValue);
			}
		}
	}

	private class SolarEnergyListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (solarEnergyLast != null) {
				double hours = ((double) value.getTime() - (double) solarEnergyLast.getTime())/3600000;
				if (hours > 0) {
					Value power = new DoubleValue((value.doubleValue() - solarEnergyLast.doubleValue())*1000/hours, value.getTime());
					solarPower.setLatestValue(power);
				}
			}
			solarEnergyLast = value;
		}
	}

}
