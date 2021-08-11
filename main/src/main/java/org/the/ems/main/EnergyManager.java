/* 
 * Copyright 2016-21 ISC Konstanz
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
package org.the.ems.main;

import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentCollection;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentStatus;
import org.the.ems.core.ComponentType;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.SchedulableService;
import org.the.ems.core.UnknownComponentException;
import org.the.ems.core.cmpt.ApplianceService;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.cmpt.HeatingRodService;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.cmpt.VentilationService;
import org.the.ems.core.config.Configurable;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.schedule.ControlSchedule;
import org.the.ems.core.schedule.ScheduleListener;
import org.the.ems.core.schedule.ScheduleService;
import org.the.ems.main.config.ConfigurationService;

@org.osgi.service.component.annotations.Component(
	service = EnergyManagementService.class,
	immediate = true,
	property = {
		CommandProcessor.COMMAND_SCOPE + ":String=th-e-ems",
		CommandProcessor.COMMAND_FUNCTION + ":String=maintenance"
	},
	configurationPid = EnergyManager.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public final class EnergyManager extends Configurable 
		implements EnergyManagementService, ScheduleListener, Runnable {
	private final static Logger logger = LoggerFactory.getLogger(EnergyManager.class);

	public final static String ID = "ems";
	public final static String PID = "org.the.ems.core";

	private final ComponentCollection components = new ComponentCollection();

	private ControlSchedule scheduleUpdate = null;
	private ControlSchedule schedule = new ControlSchedule();

	private Thread manager = null;

	@Configuration(mandatory = false)
	private volatile int interval = 1000;

	@Configuration
	private volatile boolean maintenance = false;
	private volatile boolean deactivate;

	@Reference
	ConfigurationService configs;

	@Activate
	protected void activate(Map<String, ?> properties) {
		logger.info("Activating TH-E Energy Management System");
		try {
			configure(Configurations.create(properties));
			
			manager = new Thread(this);
			manager.setName("TH-E EMS");
			manager.start();
			
		} catch (ConfigurationException e) {
			logger.error("Error while loading configurations: {}", e.getMessage());
		}
	}

	@Modified
	void modified(Map<String, ?> properties) {
		try {
			configure(Configurations.create(properties));
			manager.interrupt();
			
		} catch (ConfigurationException e) {
			logger.warn("Error while updating configurations: {}", e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		logger.info("Deactivating TH-E Energy Management System");
		deactivate = true;
		
		manager.interrupt();
		try {
			manager.join();
			
		} catch (InterruptedException e) {
		}
	}

	@Reference(
		cardinality = ReferenceCardinality.MANDATORY,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindContentManagementService(ContentManagementService service) {
		content = service;
	}

	protected void unbindContentManagementService(ContentManagementService service) {
		content = null;
	}

	@Override
	public void onScheduleReceived(ControlSchedule schedule) {
		scheduleUpdate = schedule;
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
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindApplianceService(ApplianceService applianceService) {
		bindComponentService(applianceService);
	}

	protected void unbindApplianceService(ApplianceService applianceService) {
		unbindComponentService(applianceService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindElectricVehicleService(ElectricVehicleService electricVehicleService) {
		bindComponentService(electricVehicleService);
	}

	protected void unbindElectricVehicleService(ElectricVehicleService electricVehicleService) {
		unbindComponentService(electricVehicleService);
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
	protected void bindHeatingRodService(HeatingRodService heatingRodService) {
		bindComponentService(heatingRodService);
	}

	protected void unbindHeatingRodService(HeatingRodService heatingRodService) {
		unbindComponentService(heatingRodService);
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
	protected void bindThermalStorageService(ThermalEnergyStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindThermalStorageService(ThermalEnergyStorageService storageService) {
		unbindComponentService(storageService);
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
	protected void bindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		synchronized (components) {
			if (!components.containsKey(id)) {
				logger.info("Registered TH-E EMS {}: {}", 
						componentService.getType().getFullName(), id);
				
				components.put(id, componentService);
				manager.interrupt();
			}
		}
	}

	protected void unbindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		synchronized (components) {
			logger.info("Deregistered TH-E EMS {}: {}", 
					componentService.getType().getFullName(), id);
			
			components.remove(id);
		}
	}

	@Override
	public ComponentService getComponent(String id) throws UnknownComponentException {
		if (!components.containsKey(id)) {
			throw new UnknownComponentException("Unknown component for id: " + id);
		}
		return components.get(id);
	}

	@Override
	public List<ComponentService> getComponents(ComponentType... types) {
		return components.getAll(types);
	}

	@Override
	public void setMaintenance(boolean enabled) throws EnergyManagementException {
		maintenance = enabled;
		manager.interrupt();

		logger.info(maintenance ? "Enabling" : "Disabling" + " TH-E EMS maintenance mode");
	}

	public void maintenance(String enabled) throws EnergyManagementException {
		setMaintenance(Boolean.parseBoolean(enabled));
	}

	@Override
	public void run() {
		logger.info("Starting TH-E EMS");
		
		deactivate = false;
		while (!deactivate) {
			if (Thread.interrupted()) {
				handleInterruptEvent();
				continue;
			}
			try {
				long timestamp = System.currentTimeMillis();
				
				configs.watch();
				
				handleComponentEvent();
				
				long time = System.currentTimeMillis() - timestamp;
				long sleep = interval - time;
				if (sleep > 0) {
					Thread.sleep(sleep);
				}
			} catch (InterruptedException e) {
				handleInterruptEvent();
				continue;
			}
		}
	}

	private void handleInterruptEvent() {
		if (deactivate) {
			logger.info("TH-E EMS thread interrupted and will stop");
			return;
		}
		handleComponentEvent();
	}

	private void handleComponentEvent() {
		// TODO: implement channel flag checks, component and optimization verifications
		
		boolean scheduleFlag = false;
		if (scheduleUpdate != null && scheduleUpdate.getTimestamp() > schedule.getTimestamp()) {
			// TODO: verify schedule integrity
			scheduleFlag = true;
			schedule = scheduleUpdate;
		}
		synchronized (components) {
			for (ComponentService component : components.values()) {
				try {
					if (maintenance) {
						component.setStatus(ComponentStatus.MAINTENANCE);
					}
					else {
						// TODO: check for other possible component states
						if (component.getStatus() != ComponentStatus.ENABLED) {
							component.setStatus(ComponentStatus.ENABLED);
						}
						
						if (component instanceof SchedulableService && scheduleFlag) {
							try {
								if (schedule.contains(component)) {
									((SchedulableService) component).schedule(schedule.get(component));
								}
							} catch (ComponentException e) {
								logger.warn("Error while scheduling component \"{}\": ", component.getId(), e);
							}
						}
						if (component instanceof Component) ((Component) component).interrupt();
					}
				} catch (EnergyManagementException e) {
					logger.warn("Error while handling event for component \"{}\": ", component.getId(), e);
				}
			}
		}
	}

}
