package org.the.ems.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.cmpt.chp.serenergy.data.Request;
import org.the.ems.cmpt.chp.serenergy.data.State;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".serenergy",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Serenergy extends Cogenerator {
	private static final Logger logger = LoggerFactory.getLogger(Serenergy.class);

	@Configuration(mandatory = false)
	protected int enableDelay = 2500;

	@Configuration
	protected Channel enable;

	@Configuration
	protected Channel start;

	@Configuration
	protected Channel stop;

	@Configuration
	private Channel stackLimit;

	@Configuration
	private double stackTempMax;

	@Configuration
	private double stackTempMin;

	@Configuration
	private Channel stackTemp;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		stackTemp.registerValueListener(new StackTempListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		stackTemp.deregisterValueListeners();
	}

	@Override
	protected void onStart(WriteContainer container, StartSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		container.add(enable, Request.ENABLE.encode(time));
		container.add(start, Request.START.encode(time+enableDelay));
		// TODO: set stackLimit
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		container.add(stop, Request.STOP.encode(time));
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

	private class StackTempListener implements ValueListener {

		@Override
		public void onValueReceived(Value temp) {
			if (!isMaintenance()) {
				if (temp.doubleValue() > stackTempMax) {
					circulationPump.start();
				}
				else if (temp.doubleValue() < stackTempMin) {
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
