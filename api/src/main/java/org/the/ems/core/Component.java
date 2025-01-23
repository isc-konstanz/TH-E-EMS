/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
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
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.WriteContainer;

public abstract class Component extends Configurable implements ComponentService {
	private final static Logger logger = LoggerFactory.getLogger(Component.class);

	private final static int TIMEOUT = 10;

    private ComponentContext componentContext = null;

    private volatile boolean componentActive = false;
	private volatile ComponentStatus componentStatus = ComponentStatus.DISABLED;

	@Configuration(section = Configurations.GENERAL, mandatory = false)
	private String id = getType().getKey();

	protected Component(String section) {
		super(section);
	}

	protected Component() {
		super();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public final ComponentStatus getStatus() {
		return componentStatus;
	}

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
	public boolean isEnabled() {
		return super.isEnabled() &&
				componentStatus != ComponentStatus.DISABLED;
	}

	public boolean isActive() {
		return componentActive;
	}

	@Override
	public final ComponentContext getContext() {
		return componentContext;
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	void bindContentManagementService(ContentManagementService service) {
		if (componentContext != null) {
			componentContext.setContentManagement(service);
		}
	}

	void unbindContentManagementService(ContentManagementService service) {
		if (componentContext != null) {
			componentContext.setContentManagement(null);
		}
	}

	@Override
	void doConfigure(Configurations configs) throws ConfigurationException {
		super.doConfigure(configs);
		if (!isEnabled() && 
				configs.containsKey(Configurations.GENERAL, "id")) {
			this.id = configs.get(Configurations.GENERAL, "id");
		}
	}

	@Activate
	protected final void activate(BundleContext context, Map<String, ?> properties) 
			throws org.osgi.service.component.ComponentException, ComponentException {
		try {
			doActivate(context, properties);
			
		} catch (Exception e) {
			componentStatus = ComponentStatus.DISABLED;
			logger.warn("Error while activating {} {}: {}", 
					getType().getFullName(), getId(), e.getMessage());

			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	void doActivate(BundleContext context, Map<String, ?> properties) throws ComponentException {
		componentContext = new ComponentContext(this, context);
		componentContext.activate();
		doActivate(componentContext, properties);
	}

	void doActivate(ComponentContext context, Map<String, ?> properties) throws ComponentException {
		componentStatus = ComponentStatus.ENABLED;
		
		Configurations configs = Configurations.create(properties);
		configure(configs);
		if (isEnabled()) {
			onActivate(componentContext, properties);
			onActivate(componentContext, configs);
			onActivate(configs);
			onActivate();
			
			componentActive = true;
		}
	}

	protected void onActivate(ComponentContext context, Map<String, ?> properties) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(ComponentContext context, Configurations configs) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(Configurations configs) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate() throws ComponentException {
		// Default implementation to be overridden
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		try {
			doModified(properties);
			
		} catch (Exception e) {
			logger.warn("Error while updating configurations: {}", 
					getType().getFullName(), id, e.getMessage());
		}
	}

	void doModified(Map<String, ?> properties) throws ComponentException {
		Configurations configs = Configurations.create(properties);
		configure(configs);
		if (isActive()) {
			// TODO: Remove deactivating/activating
			doDeactivate();
			doActivate(componentContext, properties);
			
			onModified(componentContext, properties);
			onModified(componentContext, configs);
			onModified(configs);
			onModified();
		}
	}

	protected void onModified(ComponentContext context, Map<String, ?> properties) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onModified(ComponentContext context, Configurations configs) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onModified(Configurations configs) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onModified() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onResume() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onPause() throws ComponentException {
		// Default implementation to be overridden
	}

	@Deactivate
	protected final void deactivate() throws org.osgi.service.component.ComponentException, ComponentException {
		try {
			doDeactivate();
			
		} catch (Exception e) {
			logger.warn("Error while deactivating {} {}: {}", 
					getType().getFullName(), id, e.getMessage());
			
			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	void doDeactivate() throws ComponentException {
		getContext().deactivate();
		if (isActive()) {
			onDeactivate();
			componentStatus = ComponentStatus.DISABLED;
			componentActive = false;
		}
	}

	protected void onDeactivate() throws ComponentException {
		// Default implementation to be overridden
	}

	final int getWriteTimeout() {
		// TODO: Make write timeout configurable
		return TIMEOUT;
	}

	protected void write(WriteContainer container) throws EnergyManagementException {
        if (container.size() < 1) {
            return;
        }
        getContext().execute(() -> {
			try {
	    		for (Channel channel : container.keySet()) {
					channel.write(container.get(channel));
	    		}
			} catch (Exception e) {
				return e;
			}
			return null;
			
        }, getWriteTimeout(), TimeUnit.SECONDS);
	}

	void interrupt() throws EnergyManagementException {
		try {
			onInterrupt();

		} catch (Exception e) {
			logger.warn("Error while interrupting {} component \"{}\": {}", 
					getType().getFullName(), getId(), e.getMessage(), e);
		}
	}

	protected void onInterrupt() throws ComponentException {
		// Default implementation to be overridden
	}

}
