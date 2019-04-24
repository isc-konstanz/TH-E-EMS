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
package org.the.ems.core.cmpt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.Schedule;

public abstract class GenericComponent implements ComponentService {
	private final static Logger logger = LoggerFactory.getLogger(GenericComponent.class);

	protected volatile ComponentStatus status = ComponentStatus.DISABLED;
	protected ContentManagementService manager;

	@Override
	public ComponentStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(ComponentStatus status) throws EnergyManagementException {
		switch(status) {
		case MAINTENANCE:
			if (this.status != ComponentStatus.MAINTENANCE) {
				onMaintenance(true);
			}
			break;
		default:
			if (this.status == ComponentStatus.MAINTENANCE) {
				onMaintenance(false);
			}
			break;
		}
		this.status = status;
	}

	public boolean isMaintenance() {
		return status == ComponentStatus.MAINTENANCE;
	}

	protected abstract void onMaintenance(boolean enabled) throws EnergyManagementException;

	public void onBind(ContentManagementService context) throws EnergyManagementException {
		this.manager = context;
	}

	@Override
	public void onReload(Configurations config) throws EnergyManagementException {
		onDeactivate();
		onActivate(config);
	}

	protected abstract void onSet(ComponentWriteContainer container, Value value) 
			throws ComponentException, UnsupportedOperationException;

	public void set(Value value) throws EnergyManagementException, UnsupportedOperationException {
		ComponentWriteContainer container = new ComponentWriteContainer();
		try {
			onSet(container, value);
			
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		}
		if (container.size() < 1) {
			return;
		}
		
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

	public void schedule(Schedule schedule) throws EnergyManagementException, UnsupportedOperationException {
		ComponentWriteContainer container = new ComponentWriteContainer();
		try {
			for (Value value : schedule) {
				onSet(container, value);
			}
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		}

		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

}
