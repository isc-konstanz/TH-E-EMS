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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.config.Configurations;

public final class ComponentContext extends ConfigurableContext {
	private final static Logger logger = LoggerFactory.getLogger(ComponentContext.class);

	private final BundleContext context;

    private final Component component;

	private final Map<String, ComponentRegistration> componentRegistrations = 
			new HashMap<String, ComponentRegistration>();

	private ExecutorService executor = null;

	ComponentContext(Component component, BundleContext context) {
		super(component, context.getService(context.getServiceReference(ContentManagementService.class)));
		this.component = component;
		this.context = context;
	}

	void activate() {
		executor = Executors.newSingleThreadExecutor(new ComponentThreadFactory());
	}

	void deactivate() throws org.osgi.service.component.ComponentException, ComponentException {
		deregisterValueListeners();
		deregisterServices();
		executor.shutdown();
		executor = null;
	}

	public ComponentService getComponent() {
		return component;
	}

	public EnergyManagementService getEnergyManager() {
		return context.getService(context.getServiceReference(
				EnergyManagementService.class));
	}

	ExecutorService getExecutor() throws ComponentException {
		if (executor == null) throw new ComponentException("Executor deactivated for Component " + getComponent().getId());
		return executor;
	}

	void execute(Callable<Throwable> task, int timeout, TimeUnit unit) throws EnergyManagementException {
        final Future<Throwable> future = getExecutor().submit(task);
        Throwable err = null;
        try {
        	err = future.get(timeout, unit);
			
		} catch (InterruptedException e) {
			throw new EnergyManagementException(String.format("Writing values interrupted for {}: {}", 
					getComponent().getType().getFullName(), getComponent().getId()));
			
		} catch (TimeoutException e) {
			throw new EnergyManagementException(String.format("Writing values timed out for {}: {}", 
					getComponent().getType().getFullName(), getComponent().getId()));
			
		} catch (ExecutionException e) {
			throw new EnergyManagementException(e.getCause());
			
		} finally {
			future.cancel(true);
		}
        if (err != null) {
        	if (err instanceof EnergyManagementException) {
        		throw (EnergyManagementException) err;
        	}
        	throw new EnergyManagementException(err);
        }
	}

	BundleContext getBundleContext() {
		return context;
	}

	public void registerService(String id, Configurations configs, 
			Class<? extends Component> componentClass) throws ComponentException {
		registerService(id, configs, componentClass, ComponentService.class);
	}

	public void registerService(String id, Configurations configs, 
			Component component) throws ComponentException {
		registerService(id, configs, component, ComponentService.class);
	}

	public <C extends ComponentService> void registerService(String id, Configurations configs, 
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
	public <C extends ComponentService> void registerService(String id, Configurations configs, 
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
		component.activate(context, componentConfigs);
		
		ServiceRegistration<C> serviceRegistration = context.registerService(service, (C) component, componentConfigs);
		ComponentRegistration componentRegistration = new ComponentRegistration(serviceRegistration, component);
		componentRegistrations.put(id, componentRegistration);
	}

	public void deregisterService(String id) 
			throws org.osgi.service.component.ComponentException, ComponentException {
		if (componentRegistrations.containsKey(id)) {
			componentRegistrations.get(id).deregister();
		}
		else {
			logger.warn("Unable to unregister service for id: {}", id);
		}
	}

	public void deregisterServices() 
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

	private class ComponentThreadFactory implements ThreadFactory {

		String threadName = String.format("TH-E EMS Component {} worker", getComponent().getId());

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, threadName);
		}
	}

}
