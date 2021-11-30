package org.the.cmpt.chp.serenergy;

import java.net.Authenticator.RequestorType;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.cmpt.chp.serenergy.data.Request;
import org.the.cmpt.chp.serenergy.data.State;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentCollection;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.ValueType;
import org.the.ems.core.data.WriteContainer;

@Component(scope = ServiceScope.BUNDLE, service = CogeneratorService.class, configurationPid = CogeneratorService.PID
		+ ".serenergy", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Serenergy extends Cogenerator {

	private static final Logger logger = LoggerFactory.getLogger(Serenergy.class);
	private final ComponentCollection storages = new ComponentCollection();
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

	@Configuration
	private Channel stackLimit;

	@Configuration
	private double stackTempMax;

	@Configuration
	private double stackTempMin;

	@Configuration
	private ChannelListener stackTemp;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);

		stackTemp.registerValueListener(new StackTempListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();

		stackTemp.deregister();
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();

		container.add(enable, Request.ENABLE.encode(time));
		container.add(start, Request.START.encode(time + enableDelay));
		// TODO: set stackLimit
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		container.add(enable, Request.DISABLE.encode(time));
//		container.add(stop, Request.STOP.encode(time));
		// TODO: reset stackLimit
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		// TODO: set stackLimit
	}

	@Override
	protected void onStateChanged(Value value) throws EnergyManagementException {
		// The parent implementation would start the circulation pump here.
		// This is not needed for the Serenergy fuel cell, as the stack needs
		// to be on operating temperature first.
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
				logger.info("Registered {} in Serenergy component: {}", componentService.getType().getFullName(), id);

				storages.put(id, componentService);
			}
		}
	}

	protected void unbindComponentService(ElectricalEnergyStorageService componentService) {
		String id = componentService.getId();

		synchronized (storages) {
			logger.info("Deregistered TH-E EMS in Serenergy component {}: {}", componentService.getType().getFullName(),
					id);

			storages.remove(id);
		}
	}

	private class StackTempListener implements ValueListener {

		@Override
		public void onValueReceived(Value temp) {
			if (!isMaintenance()) {
				if (temp.doubleValue() > stackTempMax) {
					circulationPump.start();
				} else if (temp.doubleValue() < stackTempMin) {
					if (circulationPump.hasRunMinimum()) {
						circulationPump.stop();
					}
					try {
						if (State.decode(state.getLatestValue()) == State.STANDBY) {
							enable.write(Request.DISABLE.encode());
						}
					} catch (InvalidValueException e) {
						logger.debug("Error while checking standby state: {}", e.getMessage());
					}
				}
			}
		}
	}

}
