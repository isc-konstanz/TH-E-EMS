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
package org.the.ems.cmpt;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.Schedule;

public abstract class ConfiguredComponent extends ConfigurationHandler implements ManagedComponent {

	protected volatile ComponentStatus componentStatus = ComponentStatus.DISABLED;

	@Override
	public final ComponentStatus getStatus() {
		return componentStatus;
	}

	@Override
	public void setStatus(ComponentStatus status) throws EnergyManagementException {
		switch(status) {
		case MAINTENANCE:
			if (this.componentStatus != ComponentStatus.MAINTENANCE) {
				onPause();
			}
			break;
		default:
			if (this.componentStatus == ComponentStatus.MAINTENANCE) {
				onResume();
			}
			break;
		}
		this.componentStatus = status;
	}

	public final boolean isMaintenance() {
		return componentStatus == ComponentStatus.MAINTENANCE;
	}

	@Override
	public void onBind(ContentManagementService context) throws EnergyManagementException {
		super.onBind(context);
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onConfigure(configs);
	}

	@Override
	public void onResume() throws EnergyManagementException {
	}

	@Override
	public void onPause() throws EnergyManagementException {
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
	}

	@Override
	public void onDestroy() throws EnergyManagementException {
		// Clear up resources
		this.context = null;
	}

	@Override
	public final void schedule(Schedule schedule)
			throws UnsupportedOperationException, EnergyManagementException {
		
		if (isMaintenance()) {
			throw new MaintenanceException("Unable to schedule component while in maintenance");
		}
		WriteContainer container = new WriteContainer();
		for (Value value : schedule) {
			doSet(container, value);
		}
		doWrite(container);
	}

	@Override
	public void doSchedule(WriteContainer container, Schedule schedule) 
			throws UnsupportedOperationException, EnergyManagementException {
		
		for (Value value : schedule) {
			doSet(container, value);
		}
		onSchedule(container, schedule);
	}

	protected void onSchedule(WriteContainer container, Schedule schedule) 
			throws UnsupportedOperationException, ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public final void set(Value value) 
			throws UnsupportedOperationException, EnergyManagementException {
		
		WriteContainer container = new WriteContainer();
		doSet(container, value);
		doWrite(container);
	}

	@Override
	public void doSet(WriteContainer container, Value value)
			throws UnsupportedOperationException, EnergyManagementException {
		
		onSet(container, value);
	}

	protected void onSet(WriteContainer container, Value value)
			throws UnsupportedOperationException, EnergyManagementException {
		// Default implementation to be overridden
		throw new UnsupportedOperationException();
	}

	protected void doWrite(WriteContainer container) throws EnergyManagementException {
		if (container.size() < 1) {
			return;
		}
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

}
