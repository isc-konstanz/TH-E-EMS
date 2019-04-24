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
package org.the.ems.core.cmpt.inv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.InverterService;
import org.the.ems.core.cmpt.GenericComponent;
import org.the.ems.core.cmpt.inv.cons.Consumption;
import org.the.ems.core.cmpt.inv.ext.ExternalSolar;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public abstract class InverterComponent extends GenericComponent implements InverterService, InverterCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(InverterComponent.class);

	protected ExternalSolar solar;
	protected Consumption consumption;

	protected ChannelListener setpoint;
	protected int setpointMax;
	protected int setpointMin;
	protected Value setpointValue = DoubleValue.emptyValue();

	protected class SetpointListener extends ChannelListener {
		public SetpointListener(Channel channel) {
			super(channel);
		}
		
		@Override
		public void onValueReceived(Value setpoint) {
			if (setpointValue.doubleValue() != setpoint.doubleValue()) {
				setpointValue = setpoint;
				onSetpointUpdate();
			}
		}
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		InverterConfig config = configs.getSection(InverterConfig.class);
		
		setpointMax = config.getPowerMax();
		setpointMin = config.getPowerMin();
		setpoint = new SetpointListener(manager.getChannel(config.getSetpoint()));
		
		consumption = new Consumption(this, manager, config, configs);
		solar = new ExternalSolar(manager, configs);
	}

	@Override
	public void onDeactivate() {
		solar.deactivate();
		consumption.deactivate();
		setpoint.deregister();
	}

	@Override
	protected void onMaintenance(boolean enabled) throws EnergyManagementException {
		if (enabled) {
			set(InverterConfig.SETPOINT_DEFAULT);
		}
		solar.setEnabled(!enabled);
	}

	@Override
	public void onSetpointUpdate() {
		try {
			ComponentWriteContainer container = new ComponentWriteContainer();
			
			onSet(container, setpointValue);
			if (container.size() < 1) {
				return;
			}
			for (Channel channel : container.keySet()) {
				channel.write(container.get(channel));
			}
		} catch (EnergyManagementException e) {
			logger.debug("Unable to updating inverter setpoint: {}", e.getMessage());
		}
	}

	@Override
	public void onSet(ComponentWriteContainer container, Value value) throws ComponentException {
		if (value.doubleValue() != setpointValue.doubleValue()) {
			this.setpoint.getChannel().setLatestValue(value);
			return;
		}
		if (value.doubleValue() > setpointMax || value.doubleValue() < setpointMin) {
			throw new ComponentException("Inverter setpoint out of bounds: " + value);
		}
		double setpoint = value.doubleValue();
		if (solar.isEnabled()) {
			setpoint += solar.getSolar().doubleValue();
		}
		
		// TODO: Reset if setpoint is 0
		
		if (setpoint > setpointMax) {
			setpoint = setpointMax;
		}
		else if (setpoint < setpointMin) {
			setpoint = setpointMin;
		}
		set(container, new DoubleValue(setpoint, value.getTime()));
	}

	public abstract void set(ComponentWriteContainer container, Value value) throws ComponentException;

}
