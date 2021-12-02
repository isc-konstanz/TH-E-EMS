package org.the.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentCollection;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.RunState;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(scope = ServiceScope.BUNDLE, service = CogeneratorService.class, configurationPid = CogeneratorService.PID
		+ ".cabinet", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SerenergyCabinet extends Cogenerator {
	private static final Logger logger = LoggerFactory.getLogger(SerenergyCabinet.class);
	private final ComponentCollection storages = new ComponentCollection();

	@Configuration(mandatory = false)
	protected int enableDelay = 2500;

	@Configuration(mandatory = false)
	protected int socStartRequest = 15;

	@Configuration
	protected Channel enable;

	@Configuration
	protected Channel start;

	@Configuration
	protected Channel stop;

	@Configuration(mandatory = false)
	protected Channel status;

	@Configuration(mandatory = false)
	private ChannelListener eesSoc;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		if (eesSoc != null) {
			eesSoc.registerValueListener(new StateOfChargeListener());
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		if (eesSoc != null) {
			eesSoc.deregister();
			eesSoc = null;
		}

	}

	@Override
	protected void onStart(WriteContainer container, Value value) {
		long time = value.getTime();

		container.addBoolean(enable, true, time);
		container.addBoolean(start, true, time + 500);
		container.addBoolean(stop, false, time + 500);

		try {
			doWrite(container);
		} catch (EnergyManagementException e) {
			logger.warn("Error starting CHP: {}", e.getMessage());
		}
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		container.addBoolean(enable, false, time);
		container.addBoolean(stop, true, time + 500);
		container.addBoolean(start, false, time + 500);

		try {
			doWrite(container);
		} catch (EnergyManagementException e) {
			logger.warn("Error stopping CHP: {}", e.getMessage());
		}
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void bindElectricalStorageService(ElectricalEnergyStorageService storageService) {
		bindComponentService(storageService);
	}

	protected void unbindElectricalStorageService(ElectricalEnergyStorageService storageService) {
		unbindComponentService(storageService);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void bindComponentService(ElectricalEnergyStorageService componentService) {
		String id = componentService.getId();

		synchronized (storages) {
			if (!storages.containsKey(id)) {
				logger.debug("Registered {} in Serenergy component: {}", componentService.getType().getFullName(), id);
				storages.put(id, componentService);
			}
		}
	}

	protected void unbindComponentService(ElectricalEnergyStorageService componentService) {
		String id = componentService.getId();

		synchronized (storages) {
			logger.debug("Deregistered TH-E EMS in Serenergy component {}: {}",
					componentService.getType().getFullName(), id);
			storages.remove(id);
		}
	}

	private class StateOfChargeListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			ElectricalEnergyStorageService eesService = (ElectricalEnergyStorageService) storages
					.get(ComponentType.ELECTRICAL_ENERGY_STORAGE);

			try {
				if (value.doubleValue() >= eesService.getMaxStateOfCharge() && getState() == RunState.RUNNING) {
					stop();
					logger.info("Stopping CHP due to maximal SOC boundary reached.");

				} else if (value.doubleValue() <= socStartRequest && getState() != RunState.STARTING
						&& getState() != RunState.RUNNING) {
					start();
					logger.info("Starting CHP due to minimal SOC boundary reached.");
				}
			} catch (Exception e) {
				logger.warn("Error in SerenergyComponent: {}", e.getMessage());
			}

		}
	}
}
