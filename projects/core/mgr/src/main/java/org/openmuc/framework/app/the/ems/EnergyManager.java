/* 
 * Copyright 2016-18 ISC Konstanz
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
package org.openmuc.framework.app.the.ems;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

import org.apache.felix.service.command.CommandProcessor;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.CogeneratorService;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.ElectricalStorageService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.InverterService;
import org.the.ems.core.ThermalStorageService;
import org.the.ems.core.VentilationService;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
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
public final class EnergyManager extends Thread implements ScheduleListener {
	private final static Logger logger = LoggerFactory.getLogger(EnergyManager.class);

	private final static int SLEEP_INTERVAL = 60000;
	private final static String CONFIG_MANAGEMENT = "de.thebox.control.config.ems";
	private final static String CONFIG_COMPONENTS = "de.thebox.control.config.components";

	@Reference
	private ContentManagementService cms;

	private final Map<String, ComponentService> components = new HashMap<String, ComponentService>();
	private final Map<String, ComponentService> newComponents = new LinkedHashMap<String, ComponentService>();

	private ControlSchedule schedule = new ControlSchedule();
	private ControlSchedule newSchedule = null;

	private ChannelListener maintenance = null;
	private volatile boolean maintenanceFlag;
	private volatile boolean deactivateFlag;

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E Energy Management System");
		try {
			activateListeners(loadEnergyManagementConfig());
			
		} catch (EnergyManagementException e) {
			logger.error("Error while reading ems configuration: {}", e.getMessage());
		}
		
		start();
	}

	protected void activateListeners(Configurations configs) throws EnergyManagementException {
		EnergyManagementConfig config = configs.getSection(EnergyManagementConfig.class);
		activateMaintenanceListener(config);
	}

	protected void activateMaintenanceListener(EnergyManagementConfig configs) throws EnergyManagementException {
		if (maintenance != null) {
			maintenance.deregister();
		}
		maintenance = new ChannelListener(cms.getChannel(configs.getMaintenance())) {
			
			@Override
			public void onValueReceived(Value value) {
				maintenanceFlag = value.booleanValue();
				interrupt();
			}
		};
		
		maintenanceFlag = false;
		maintenance.getChannel().setLatestValue(new BooleanValue(maintenanceFlag));
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E Energy Management System");
		deactivateFlag = true;

		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void onScheduleReceived(ControlSchedule schedule) {
		newSchedule = schedule;
		interrupt();
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
	protected void bindElectricalStorageService(ElectricalStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindElectricalStorageService(ElectricalStorageService storageService) {
		unbindComponentService(storageService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindThermalStorageService(ThermalStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindThermalStorageService(ThermalStorageService storageService) {
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

	protected void bindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		synchronized (newComponents) {
			if (!newComponents.containsKey(id) && !components.containsKey(id)) {
				logger.info("Registering TH-E EMS {}: {}", componentService.getClass().getInterfaces()[0].getSimpleName(), id);
				
				newComponents.put(id, componentService);
				interrupt();
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
		if (removedComponent != null) {
			logger.info("Deregistering TH-E EMS {}: {}", componentService.getClass().getInterfaces()[0].getSimpleName(), id);
			
			removedComponent.onDeactivate();
		}
		else {
			// Component was removed before it was added to the active components
			newComponents.remove(id);
		}
		logger.debug("Component deregistered: " + id);
	}

	private Configurations loadComponentConfig(ComponentService component) throws ConfigurationException {
		String id = component.getId().toLowerCase().replaceAll("[^A-Za-z0-9]", "-");
		
		String fileDir = System.getProperty(CONFIG_COMPONENTS);
		if (fileDir == null) {
			fileDir = "conf" + File.separator + "ems" + File.separator;
		}
		if (!fileDir.endsWith(File.separator)) {
			fileDir += File.separator;
		}
		String fileName = fileDir + component.getType().getKey() + File.separator + id + ".cfg";
		try {
			Ini ini = new Ini(new File(fileName));
			Preferences prefs = new IniPreferences(ini);
			return new Configurations(prefs);
			
		} catch (IOException e) {
			throw new ConfigurationException("Error while reading component configuration: " + e.getMessage());
		}
	}

	private Configurations loadEnergyManagementConfig() throws ConfigurationException {
		String fileName = System.getProperty(CONFIG_MANAGEMENT);
		if (fileName == null) {
			fileName = "conf" + File.separator + "ems" + File.separator + "th-e-ems.cfg";
		}
		try {
			Ini ini = new Ini(new File(fileName));
			Preferences prefs = new IniPreferences(ini);
			return new Configurations(prefs);
			
		} catch (IOException e) {
			throw new ConfigurationException("Error while reading configuration: " + e.getMessage());
		}
	}

	public void reload() {
		logger.info("Reload TH-E EMS configuration.");
		// TODO: implement reload
		try {
			activateListeners(loadEnergyManagementConfig());
			
		} catch (EnergyManagementException e) {
			logger.error("Error while reloading TH-E EMS configuration: {}", e.getMessage());
		}
		for (ComponentService component : components.values()) {
			try {
				component.onReload(loadComponentConfig(component));
				
			} catch (EnergyManagementException e) {
				logger.error("Error while reloading TH-E EMS component \"{}\": {}", component.getId(), e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		setName("TH-E EMS");
		
		logger.info("Starting TH-E EMS");
		
		deactivateFlag = false;
		while (!deactivateFlag) {
			if (interrupted()) {
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
						component.onBind(cms);
						component.onActivate(loadComponentConfig(component));
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
