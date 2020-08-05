/* 
 * Copyright 2016-20 ISC Konstanz
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
package org.the.ems.core;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.config.Configurable;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;

public abstract class Component extends Configurable implements ComponentService {
	private final static Logger logger = LoggerFactory.getLogger(Component.class);

	protected volatile ComponentStatus componentStatus = ComponentStatus.DISABLED;

	@Configuration(section = Configurations.GENERAL, mandatory = false)
	protected String id = getType().getKey().toUpperCase();

	@Override
	public String getId() {
		return id;
	}

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

	@Activate
	protected final void activate(BundleContext context, Map<String, ?> properties) throws org.osgi.service.component.ComponentException {
		try {
			super.activate(context.getService(context.getServiceReference(ContentManagementService.class)));
			
			Configurations configs = Configurations.create(properties);
			configure(configs);
			
			onActivate(configs, context);
			onActivate(configs);
			
		} catch (Exception e) {
			logger.warn("Error while activating {} {}: {}", 
					getType().getFullName(), getId(), e.getMessage());
			
			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		try {
			Configurations configs = Configurations.create(properties);
			
			onDeactivate();
			configure(configs);
			onActivate(configs);
			
		} catch (Exception e) {
			logger.warn("Error while updating configurations: {}", e.getMessage());
		}
	}

	@Deactivate
	protected final void deactivate() {
		// Clear up resources
		this.content = null;
		try {
			onDeactivate();
			
		} catch (Exception e) {
			logger.warn("Error while deactivating {} {}: {}", 
					getType().getFullName(), id, e.getMessage());
			
			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	protected void onActivate(Configurations configs, BundleContext context) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(Configurations configs) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onResume() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onPause() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onDeactivate() throws ComponentException {
		// Default implementation to be overridden
	}

}
