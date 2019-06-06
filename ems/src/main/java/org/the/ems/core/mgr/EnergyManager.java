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
package org.the.ems.core.mgr;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.cmpt.ManagedComponent;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.cmpt.VentilationService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.mgr.config.IniConfigurations;
import org.the.ems.core.schedule.ControlSchedule;
import org.the.ems.core.schedule.ScheduleListener;
import org.the.ems.core.schedule.ScheduleService;

@Component(
	immediate = true,
	property = {
		CommandProcessor.COMMAND_SCOPE + ":String=th-e-ems",
		CommandProcessor.COMMAND_FUNCTION + ":String=reload"
	},
	service = {}
)
public final class EnergyManager extends ConfigurationHandler implements ScheduleListener, Runnable {
	private final static Logger logger = LoggerFactory.getLogger(EnergyManager.class);

	private final static int SLEEP_INTERVAL = 60000;
	private final static String CONFIG_MANAGEMENT = "de.the.ems.core.config";
	private final static String CONFIG_COMPONENTS = "de.the.ems.cmpt.config";

	private final Map<String, ComponentService> components = new HashMap<String, ComponentService>();
	private final Map<String, ComponentService> newComponents = new LinkedHashMap<String, ComponentService>();

	private ControlSchedule schedule = new ControlSchedule();
	private ControlSchedule newSchedule = null;

	@Configuration(mandatory = false)
	private ChannelListener maintenance;
	private volatile boolean maintenanceFlag;
	private volatile boolean deactivateFlag;

	private Thread manager;

	@Reference
	protected ContentManagementService context;

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E Energy Management System");
		this.onConfigure();
		
		manager = new Thread(this);
		manager.setName("TH-E EMS");
		manager.start();
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E Energy Management System");
		deactivateFlag = true;
		
		manager.interrupt();
		try {
			manager.join();
			
		} catch (InterruptedException e) {
		}
	}

	protected void onConfigure() {
		String fileName = System.getProperty(CONFIG_MANAGEMENT);
		if (fileName == null) {
			fileName = "conf" + File.separator + "ems" + File.separator + "th-e-ems.cfg";
		}
		try {
			super.onConfigure(new IniConfigurations(fileName));
			
		} catch (ConfigurationException e) {
			logger.error("Error while reading ems configuration: {}", e.getMessage());
		}
		
		if (maintenance == null) {
			return;
		}
		else {
			maintenance.deregister();
		}
		maintenance.registerValueListener(new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				maintenanceFlag = value.booleanValue();
				manager.interrupt();
			}
		});
		
		maintenanceFlag = false;
		maintenance.setLatestValue(new BooleanValue(maintenanceFlag));
	}

	@Override
	public void onScheduleReceived(ControlSchedule schedule) {
		newSchedule = schedule;
		manager.interrupt();
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindScheduleService(ScheduleService scheduleService) {
		onScheduleReceived(scheduleService.getSchedule(this));
	}

	protected void unbindScheduleService(ScheduleService scheduleService) {
		scheduleService.deregisterScheduleListener(this);
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindInverterService(InverterService inverterService) {
		bindComponentService(inverterService);
	}

	protected void unbindInverterService(InverterService inverterService) {
		unbindComponentService(inverterService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindElectricalStorageService(ElectricalEnergyStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindElectricalStorageService(ElectricalEnergyStorageService storageService) {
		unbindComponentService(storageService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindThermalStorageService(ThermalEnergyStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindThermalStorageService(ThermalEnergyStorageService storageService) {
		unbindComponentService(storageService);
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindCogeneratorService(CogeneratorService cogeneratorService) {
		bindComponentService(cogeneratorService);
	}

	protected void unbindCogeneratorService(CogeneratorService cogeneratorService) {
		unbindComponentService(cogeneratorService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindHeatPumpService(HeatPumpService heatPumpService) {
		bindComponentService(heatPumpService);
	}

	protected void unbindHeatPumpService(HeatPumpService heatPumpService) {
		unbindComponentService(heatPumpService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindVentilationService(VentilationService ventilationService) {
		bindComponentService(ventilationService);
	}

	protected void unbindVentilationService(VentilationService ventilationService) {
		unbindComponentService(ventilationService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		synchronized (newComponents) {
			if (!newComponents.containsKey(id) && !components.containsKey(id)) {
				logger.info("Registering TH-E EMS {}: {}", componentService.getClass().getInterfaces()[0].getSimpleName(), id);
				
				newComponents.put(id, componentService);
				manager.interrupt();
			}
		}
	}

	protected void unbindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		// OSGi deactivate functions are always called sequentially:
		ComponentService removedComponent;
		synchronized (components) {
			removedComponent = components.remove(id);
		}
		String removedComponentType = componentService.getClass().getInterfaces()[0].getSimpleName();
		try {
			if (removedComponent != null) {
				logger.info("Deregistering TH-E EMS {}: {}", removedComponentType, id);
				if (removedComponent instanceof ManagedComponent) {
					ManagedComponent managedComponent = (ManagedComponent) removedComponent;
					managedComponent.onDeactivate();
					managedComponent.onDestroy();
				}
			}
			else {
				// Component was removed before it was added to the active components
				newComponents.remove(id);
			}
			logger.debug("Component deregistered: " + id);
			
		} catch (EnergyManagementException e) {
			logger.warn("Error deregistering TH-E EMS {} \"{}\": {}", removedComponentType, id, 
					e.getMessage());
		}
	}

	private Configurations loadComponentConfigs(ComponentService component) throws ConfigurationException {
		String id = component.getId().toLowerCase().replaceAll("[^A-Za-z0-9]", "-");
		
		String fileDir = System.getProperty(CONFIG_COMPONENTS);
		if (fileDir == null) {
			fileDir = "conf" + File.separator + "ems" + File.separator;
		}
		if (!fileDir.endsWith(File.separator)) {
			fileDir += File.separator;
		}
		String fileName = fileDir + component.getType().getKey() + File.separator + id + ".cfg";
		
		return new IniConfigurations(fileName);
	}

	public void reload() {
		logger.info("Reload TH-E EMS configuration.");
		this.onConfigure();
		
		for (ComponentService component : components.values()) {
			if (component instanceof ManagedComponent) {
				try {
					ManagedComponent managedComponent = (ManagedComponent) component;
					managedComponent.onDeactivate();
					managedComponent.onActivate(loadComponentConfigs(managedComponent));
					
				} catch (EnergyManagementException e) {
					logger.error("Error while reloading TH-E EMS component \"{}\": {}", 
							component.getId(), e.getMessage());
				}
			}
		}
	}

	@Override
	public void run() {
		logger.info("Starting TH-E EMS");
		
		deactivateFlag = false;
		while (!deactivateFlag) {
			if (manager.isInterrupted()) {
				handleInterruptEvent();
				continue;
			}
			try {
				handleComponentEvent();
				Thread.sleep(SLEEP_INTERVAL);
				
			} catch (InterruptedException e) {
				handleInterruptEvent();
				continue;
			}
		}
	}

	private void handleInterruptEvent() {
		if (deactivateFlag) {
			logger.info("TH-E EMS thread interrupted and will stop");
			return;
		}
		
		synchronized (newComponents) {
			if (newComponents.size() != 0) {
				synchronized (components) {
					components.putAll(newComponents);
				}
				for (Entry<String, ComponentService> newComponentEntry : newComponents.entrySet()) {
					String id = newComponentEntry.getKey();
					ComponentService component = newComponentEntry.getValue();
					
					logger.info("Activating TH-E EMS {}: {}", component.getClass().getInterfaces()[0].getSimpleName(), id);
					try {
						if (component instanceof ManagedComponent) {
							ManagedComponent managedComponent = (ManagedComponent) component;
							managedComponent.onBind(context);
							managedComponent.onActivate(loadComponentConfigs(managedComponent));
						}
						component.setStatus(ComponentStatus.ENABLED);
						
					} catch (EnergyManagementException e) {
						logger.warn("Error while activating component \"{}\": ", id, e);
					}
				}
				newComponents.clear();
			}
		}
		handleComponentEvent();
	}

	private void handleComponentEvent() {
		// TODO: implement channel flag checks, component and optimization verifications
		
		boolean scheduleFlag = false;
		if (newSchedule != null && newSchedule.getTimestamp() > schedule.getTimestamp()) {
			// TODO: verify schedule integrity
			scheduleFlag = true;
			schedule = newSchedule;
		}
		synchronized (components) {
			for (ComponentService component : components.values()) {
				try {
					if (maintenanceFlag) {
						component.setStatus(ComponentStatus.MAINTENANCE);
					}
					else {
						// TODO: check for other possible component states
						if (component.getStatus() != ComponentStatus.ENABLED) {
							component.setStatus(ComponentStatus.ENABLED);
						}
						
						if (scheduleFlag) {
							try {
								if (newSchedule.contains(component)) {
									component.schedule(newSchedule.get(component));
								}
							} catch (ComponentException e) {
								logger.warn("Error while scheduling component \"{}\": ", component.getId(), e);
							}
						}
					}
				} catch (EnergyManagementException e) {
					logger.warn("Error while handling event for component \"{}\": ", component.getId(), e);
				}
			}
		}
	}

}
