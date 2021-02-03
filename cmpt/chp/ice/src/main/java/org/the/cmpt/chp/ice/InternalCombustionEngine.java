package org.the.cmpt.chp.ice;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.RunState;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

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

	@Configuration(mandatory = false)
	protected Channel engineMode = null;

	@Configuration(mandatory = false, scale=1000)
	protected int engineModePriorDelay = 250;

	@Configuration(mandatory = false, scale=1000)
	protected int engineModePostDelay = 50;

	@Configuration
	protected ChannelListener engine;

	@Configuration(mandatory = false)
	protected float tempMax = 90;

	@Configuration(value="temp_max_*")
	protected ChannelCollection temperatures;

	@Configuration(value="el_power")
	protected ChannelListener power;

	@Override
	public Value getElectricalPower() throws ComponentException {
		Value powerValue = power.getLatestValue();
		if (powerValue == null) {
			throw new ComponentException("Unable to retrieve electrical power");
		}
		return powerValue;
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		engine.registerValueListener(new EngineListener());
		for (Channel temperature : temperatures.values()) {
			temperature.registerValueListener(new TemperatureListener());
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		engine.deregister();
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
	protected void onRunning() throws ComponentException {
		super.onRunning();
		try {
			setEngineMode(System.currentTimeMillis(), 
					EngineMode.DELTA);
			
		} catch (EnergyManagementException e) {
			logger.warn("Error enabling engine delta mode: {}", e.getMessage());
		}
	}

	@Override
	protected boolean isRunning() throws ComponentException {
		if (powerMin > 0) {
			try {
				return Math.abs(getElectricalPower().doubleValue()) > powerMin;
				
			} catch(ComponentException e) {
				logger.debug("Error while checking run state: {}", e.getMessage());
			}
		}
		return super.isRunning();
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		if (valve != null) {
			container.addBoolean(valve, false, time);
			time += valveDelay;
		}
		container.addBoolean(engine, false, time);
		
		// TODO: Reset power level
	}

	protected void onEngineStop(long time) throws EnergyManagementException {
		if (engineMode != null) {
			WriteContainer writeContainer = new WriteContainer();
			writeContainer.add(engineMode, EngineMode.STAR.getValue(time));
			doWrite(writeContainer);
		}
	}

	protected void setEngineMode(long time, EngineMode mode) throws EnergyManagementException {
		if (engineMode != null) {
			WriteContainer writeContainer = new WriteContainer();
			
			if (valve != null) {
				writeContainer.addBoolean(valve, false, time);
				time += engineModePriorDelay;
			}
			writeContainer.add(engineMode, mode.getValue(time));
			
			if (valve != null) {
				time += engineModePostDelay;
				writeContainer.addBoolean(valve, true, time);
			}
			doWrite(writeContainer);
		}
	}

	@Override
	protected boolean isStandby() throws ComponentException {
		try {
			return getElectricalPower().doubleValue() == 0.0;
			
		} catch(ComponentException e) {
			logger.debug("Error while checking standby state: {}", e.getMessage());
		}
		return super.isStandby();
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		// TODO: Set power level
	}

	private class EngineListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			boolean state = value.booleanValue();
			switch(getState()) {
			case STOPPING:
			case STANDBY:
				if (state) {
					setState(RunState.STARTING);
				}
				break;
			case STARTING:
			case RUNNING:
				if (!state) {
					setState(RunState.STOPPING);
				}
				break;
			}
			if (!state) {
				try {
					onEngineStop(System.currentTimeMillis());
					
				} catch (EnergyManagementException e) {
					logger.warn("Error shutting down engine: {}", e.getMessage());
				}
			}
		}
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
		}
	}

}
