package org.the.cmpt.chp.ice;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.HeatingState;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".ice",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class InternalCombustionEngine extends Cogenerator {
	private static final Logger logger = LoggerFactory.getLogger(InternalCombustionEngine.class);

	@Configuration(mandatory = false, scale=1000)
	protected int starterTime = 2500;

	@Configuration(mandatory = false)
	protected Channel starter = null;


	@Configuration(mandatory = false, scale=1000)
	protected int valveDelay = 5000;

	@Configuration(mandatory = false)
	protected Channel valve = null;


	@Configuration
	protected Channel engine;

	@Configuration(mandatory = false)
	protected float tempMax = 90;

	@Configuration(mandatory = false)
	protected float tempStop = 70;

	@Configuration(value="temp_max_*")
	protected ChannelCollection temperatures;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		// Register Channel listener
		for (Channel temperature : temperatures.values()) {
			temperature.registerValueListener(new TemperatureListener());
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		temperatures.deregister();
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
		
		container.addBoolean(engine, true, time);
		
		if (valve != null) {
			time += valveDelay;
			container.addBoolean(valve, true, time);
		}
		if (starter != null) {
			container.addBoolean(starter, true, time);
			container.addBoolean(starter, false, time+starterTime);
			
		}
		// TODO: Set power level
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		if (valve != null) {
			container.addBoolean(valve, false, time);
		}
		// TODO: Reset power level
	}

	protected void stopEngine(long time) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		
		writeContainer.addBoolean(engine, false, time);
		doWrite(writeContainer);
		setState(HeatingState.STANDBY);
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		// TODO: Set power level
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (value.floatValue() > tempMax) {
				logger.debug("Temperature in cooling circulation or motor above threshold. Shutting down CHP");
				try {
					doStop(System.currentTimeMillis());
					
				} catch (EnergyManagementException e) {
					logger.warn("Error while trying to shut down CHP: {}", e.getMessage());
				}
			}
			else if (engine.getLatestValue().booleanValue()) {
				switch(getState()) {
				case STOPPING:
				case STANDBY:
					if (value.floatValue() < tempStop) {
						long timestamp = System.currentTimeMillis();
						if (timestamp - stopTimeLast > valveDelay) {
							try {
								stopEngine(timestamp);
								
							} catch (EnergyManagementException e) {
								logger.warn("Failed to stop engine: {}", e.getMessage());
							}
						}
					}
					break;
				default:
					break;
				}
			}
		}
	}

}
