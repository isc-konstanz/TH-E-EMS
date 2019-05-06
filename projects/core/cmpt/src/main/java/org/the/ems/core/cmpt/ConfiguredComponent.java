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
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.Schedule;

public abstract class ConfiguredComponent extends ConfigurationHandler implements ManagedComponent {
	private final static Logger logger = LoggerFactory.getLogger(ConfiguredComponent.class);

	protected volatile ComponentStatus status = ComponentStatus.DISABLED;

	@Override
	public ComponentStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(ComponentStatus status) throws EnergyManagementException {
		switch(status) {
		case MAINTENANCE:
			if (this.status != ComponentStatus.MAINTENANCE) {
				onPause();
			}
			break;
		default:
			if (this.status == ComponentStatus.MAINTENANCE) {
				onResume();
			}
			break;
		}
		this.status = status;
	}

	public boolean isMaintenance() {
		return status == ComponentStatus.MAINTENANCE;
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

	@Override
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
