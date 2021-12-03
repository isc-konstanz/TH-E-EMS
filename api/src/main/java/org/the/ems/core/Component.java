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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.WriteContainer;

public abstract class Component extends Configurable implements ComponentService {
	private final static Logger logger = LoggerFactory.getLogger(Component.class);

	private final Map<String, ComponentRegistration> componentRegistrations = 
			new HashMap<String, ComponentRegistration>();

	private ContentManagementService content;

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
	protected final ContentManagementService getContentManagement() {
		return content;
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindContentManagementService(ContentManagementService service) {
		content = service;
	}

	protected void unbindContentManagementService(ContentManagementService service) {
		content = null;
	}

	@Activate
	protected final void activate(BundleContext context, Map<String, ?> properties) 
			throws org.osgi.service.component.ComponentException, ComponentException {
		try {
			doActivate(context, properties);
			
		} catch (Exception e) {
			logger.warn("Error while activating {} {}: {}", 
					getType().getFullName(), getId(), e.getMessage());
			
			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	void doActivate(BundleContext context, Map<String, ?> properties) throws ComponentException {
		content = context.getService(context.getServiceReference(ContentManagementService.class));
		Configurations configs = Configurations.create(properties);
		configure(configs);
		onActivate(context, properties);
		onActivate(context, configs);
		onActivate(configs);
		onActivate();
	}

	protected void onActivate(BundleContext context, Map<String, ?> properties) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(BundleContext context, Configurations configs) throws ComponentException {
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
		onModified(FrameworkUtil.getBundle(getClass()).getBundleContext(), properties);
	}

	protected void onModified(BundleContext context, Map<String, ?> properties) throws ComponentException {
		Configurations configs = Configurations.create(properties);
		doDeactivate();
		onActivate(context, properties);
		onActivate(context, configs);
		onActivate(configs);
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
		onDeactivate();
		deregisterServices();
		deregisterConfiguredValueListeners();
	}

	protected void onDeactivate() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void write(WriteContainer container) throws EnergyManagementException {
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

	void interrupt() throws EnergyManagementException {
		try {
			onInterrupt();
			
		} catch (Exception e) {
			logger.warn("Error while interrupting {} {}: {}", 
					getType().getFullName(), id, e.getMessage());
		}
	}

	protected void onInterrupt() throws ComponentException {
		// Default implementation to be overridden
	}

	protected void registerService(String id, Configurations configs, 
			Class<? extends Component> componentClass) throws ComponentException {
		registerService(id, configs, componentClass, ComponentService.class);
	}

	protected void registerService(String id, Configurations configs, 
			Component component) throws ComponentException {
		registerService(id, configs, component, ComponentService.class);
	}

	protected <C extends ComponentService> void registerService(String id, Configurations configs, 
			Class<? extends Component> componentClass, Class<C> service) throws ComponentException {
		
		try {
			Constructor<? extends Component> constructor = componentClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			Component component = constructor.newInstance();
			
			registerService(id, configs, component, service);
			
		} catch (InstantiationException | InvocationTargetException | IllegalAccessException | IllegalArgumentException | 
				NoSuchMethodException | SecurityException  e) {
			throw new ComponentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected <C extends ComponentService> void registerService(String id, Configurations configs, 
			Component component, Class<C> service) throws ComponentException {
		Configurations componentConfigs = Configurations.create();
		componentConfigs.put(Configurations.GENERAL, "id", id);
		for (Entry<String, ?> entry : configs.entrySet()) {
			if (entry.getKey().toLowerCase().equals("general.id") ||
					entry.getKey().toLowerCase().startsWith("component") ||
					entry.getKey().toLowerCase().startsWith("bundle") ||
					entry.getKey().toLowerCase().startsWith("service")) {
				
				continue;
			}
			componentConfigs.put(entry.getKey(), entry.getValue());
		}
		BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		component.activate(context, componentConfigs);
		
		ServiceRegistration<C> serviceRegistration = context.registerService(service, (C) component, componentConfigs);
		ComponentRegistration componentRegistration = new ComponentRegistration(serviceRegistration, component);
		componentRegistrations.put(id, componentRegistration);
	}

	protected void deregisterService(String id) 
			throws org.osgi.service.component.ComponentException, ComponentException {
		if (componentRegistrations.containsKey(id)) {
			componentRegistrations.get(id).deregister();
		}
		else {
			logger.warn("Unable to unregister service for id: {}", id);
		}
	}

	protected void deregisterServices() 
			throws org.osgi.service.component.ComponentException, ComponentException {
		
		for (ComponentRegistration componentRegistration : componentRegistrations.values()) {
			componentRegistration.deregister();
		}
	}

	private class ComponentRegistration {

		private final Component component;
		private final ServiceRegistration<? extends ComponentService> registration;

		private ComponentRegistration(ServiceRegistration<? extends ComponentService> registration,
				Component component) {
			
			this.registration = registration;
			this.component = component;
		}

		public void deregister() throws org.osgi.service.component.ComponentException, ComponentException {
			component.deactivate();
			registration.unregister();
		}
	}
}
