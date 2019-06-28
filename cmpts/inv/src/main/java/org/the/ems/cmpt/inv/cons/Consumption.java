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
package org.the.ems.cmpt.inv.cons;

import java.util.HashMap;
import java.util.Map;

import org.the.ems.cmpt.inv.InverterCallbacks;
import org.the.ems.cmpt.inv.cons.PowerListener.PowerCallbacks;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.config.ConfiguredObject;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class Consumption extends ConfiguredObject implements PowerCallbacks, ValueListener {

	private final static String SECTION = "Consumption";

	private volatile InverterCallbacks callbacks = null;

	@Configuration(section=Configurations.GENERAL)
	private ChannelListener consPower;

	@Configuration
	private ChannelListener acPower;

	@Configuration(value= {"dc1_power", "dc_power"})
	private ChannelListener dc1Power;

	@Configuration(mandatory=false)
	private ChannelListener dc2Power;

	@Configuration
	private ChannelListener eesPower;

	private Map<PowerType, Value> powerValues = new HashMap<PowerType, Value>();

	private volatile boolean running = false;

	@Override
	@SuppressWarnings("unchecked")
	public Consumption activate(ContentManagementService content) throws ComponentException {
		super.activate(content);
		return setConfiguredSection(SECTION);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Consumption configure(Configurations configs) throws ConfigurationException {
		super.configure(configs);
		if (!isDisabled()) {
			consPower.registerValueListener(this);
			
			eesPower.registerValueListener(new PowerListener(this, PowerType.EES));
			acPower.registerValueListener(new PowerListener(this, PowerType.AC));
			dc1Power.registerValueListener(new PowerListener(this, PowerType.DC1));
			if (dc2Power != null) {
				dc2Power.registerValueListener(new PowerListener(this, PowerType.DC2));
			}
			running = true;
		}
		return this;
	}

	public Consumption register(InverterCallbacks callbacks) {
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
		consPower.deregister();
		
		if (!isDisabled()) {
			eesPower.deregister();
			acPower.deregister();
			dc1Power.deregister();
			if (dc2Power != null) {
				dc2Power.deregister();
			}
		}
		callbacks = null;
		running = false;
	}

	public Value getLatestValue() {
		return consPower.getLatestValue();
	}

	public boolean isRunning() {
		return !isDisabled() && running;
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		long time = power.getTime();
		
		powerValues.put(type, power);
		if (!isRunning()) {
			return;
		}
		for (Value value : powerValues.values()) {
			if (value.getTime() != time) {
				return;
			}
		}
		double consumption = powerValues.get(PowerType.AC).doubleValue();
		if (consumption > 0) {
			consumption += powerValues.get(PowerType.DC1).doubleValue();
			if (powerValues.containsKey(PowerType.DC2)) {
				consumption += powerValues.get(PowerType.DC2).doubleValue();
			}
		}
		consumption -= powerValues.get(PowerType.EES).doubleValue();
		
		if (consumption < 0) {
			consumption = 0;
		}
		this.consPower.setLatestValue(new DoubleValue(consumption, time));
	}

	@Override
	public void onValueReceived(Value value) {
		if (callbacks != null) {
			callbacks.onSetpointUpdate();
		}
	}

}
