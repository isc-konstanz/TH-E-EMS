package org.the.ems.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".serenergy.cabinet",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SerenergyCabinet extends Cogenerator {
	private static final Logger logger = LoggerFactory.getLogger(SerenergyCabinet.class);

	private final StateOfChargeListener storageSoCListener = new StateOfChargeListener();

	private ElectricalEnergyStorageService storage;

	@Configuration(mandatory = false)
	protected int storageSoCMin = 15;

	@Configuration(mandatory = false)
	protected int enableDelay = 2500;

	@Configuration
	protected Channel enable;

	@Configuration
	protected Channel start;

	@Configuration
	protected Channel stop;

	@Configuration(mandatory = false)
	protected Channel status;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	protected void bindElectricalStorageService(ElectricalEnergyStorageService storageService) throws ComponentException {
		logger.debug("Registered {} in Serenergy component: {}", 
				storageService.getType().getFullName(), 
				storageService.getId());

		storage = storageService;
		storage.registerStateOfChargeListener(storageSoCListener);
	}

	protected void unbindElectricalStorageService(ElectricalEnergyStorageService storageService) throws ComponentException {
		logger.debug("Deegistered {} in Serenergy component: {}", 
				storageService.getType().getFullName(), 
				storageService.getId());

		storage.deregisterStateOfChargeListener(storageSoCListener);
		storage = null;
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		if (storage != null) {
			storage.deregisterStateOfChargeListener(storageSoCListener);
		}
	}

	@Override
	protected void onStart(WriteContainer container, StartSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();

		container.addBoolean(enable, true, time);
		container.addBoolean(start, true, time+enableDelay);
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		container.addBoolean(stop, true, time);
		container.addBoolean(enable, false, time+enableDelay);
	}

	private class StateOfChargeListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			switch (getState()) {
			case RUNNING:
			case STARTING:
				logger.debug("Stopping fuel cell due to maximal state of charge threshold.");
				try {
					// TODO: Is this the right location for this? The battery may
					if (value.doubleValue() >= 100) { //storage.getMaxStateOfCharge()) {
						ValueSettings stopSettings = ValueSettings.ofBoolean(false, value.getEpochMillis());
						stopSettings.setEnforced(true);
						stop(stopSettings);
					}
				} catch (EnergyManagementException e) {
					logger.warn("Error while switching off fuel cell due to state of charge threshold violation: {}",
							e.getMessage());
				}
				break;
			case STANDBY:
			case STOPPING:
			default:
				logger.info("Starting fuel cell due to minimal state of charge threshold.");
				try {
					if (value.doubleValue() <= storageSoCMin) {
						ValueSettings startSettings = ValueSettings.ofBoolean(false, value.getEpochMillis());
						startSettings.setEnforced(true);
						start(startSettings);
					}
				} catch (EnergyManagementException e) {
					logger.warn("Error while switching on fuel cell due to state of charge threshold violation: {}",
							e.getMessage());
				}
				break;
			}
		}
	}
}
