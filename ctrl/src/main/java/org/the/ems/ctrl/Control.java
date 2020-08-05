package org.the.ems.ctrl;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentCollection;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.HeatingService;
import org.the.ems.core.HeatingState;
import org.the.ems.core.UnknownComponentException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public abstract class Control extends Component {
	private final static Logger logger = LoggerFactory.getLogger(Control.class);

	public final static String PID = "org.the.ems.ctrl";

	protected ComponentCollection components;

	@Configuration("cmpt*")
	protected List<String> componentIds;

	@Override
	public ComponentType getType() {
		return ComponentType.CONTROL;
	}

	@Override
	public void onActivate(Configurations configs, BundleContext context) throws ComponentException {
		super.onActivate(configs, context);
		logger.info("Activating TH-E EMS {}: {}", this.getClass().getSimpleName(), getId());
		
		EnergyManagementService manager = context.getService(context.getServiceReference(EnergyManagementService.class));
		for (String componentId : componentIds) {
			try {
				components.add(manager.getComponent(componentId));
				
			} catch (UnknownComponentException e) {
				throw new ConfigurationException("Unable to find controllable component: " + componentId);
			}
		}
	}

	public final void start() {
		for (HeatingService heating : components.getAll(HeatingService.class)) {
			HeatingState state = heating.getState();
			switch(state) {
			case STANDBY:
			case STOPPING:
				try {
					doStart(heating);
					onStart(heating);
				} catch (EnergyManagementException e) {
					logger.warn("Unable to start heating \"{}\": {}", heating.getId(), e.getMessage());
				}
				break;
			default:
				logger.trace("Unable to start already running heating: {}", heating.getId());
				break;
			}
		}
	}

	protected void doStart(HeatingService heating) throws EnergyManagementException {
		heating.start(getStartValue(heating));
	}

	protected void onStart(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	public final void set(double value) {
		set(new DoubleValue(value, System.currentTimeMillis()));
	}

	public final void set(Value value) {
		for (InverterService inverter : components.getAll(InverterService.class)) {
			try {
				doSet(inverter, value);
				onSet(inverter, value);
				
			} catch (EnergyManagementException e) {
				logger.warn("Unable to control inverter \"{}\": {}", inverter.getId(), e.getMessage());
			}
		}
		for (HeatingService heating : components.getAll(HeatingService.class)) {
			HeatingState state = heating.getState();
			switch(state) {
			case RUNNING:
			case STARTING:
				try {
                	doSet(heating, value);
					onSet(heating, value);
				} catch (EnergyManagementException e) {
					logger.warn("Unable to control heating \"{}\": {}", heating.getId(), e.getMessage());
				}
				break;
			default:
				logger.trace("Unable to set power value of not running heating: {}", heating.getId());
				break;
			}
		}
	}

	protected void doSet(InverterService inverter, Value value) throws EnergyManagementException {
		inverter.set(value);
	}

	protected void onSet(InverterService heating, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected void doSet(HeatingService heating, Value value) throws EnergyManagementException {
		heating.set(value);
	}

	protected void onSet(HeatingService heating, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	public final void stop() {
		for (HeatingService heating : components.getAll(HeatingService.class)) {
			HeatingState state = heating.getState();
			switch(state) {
			case RUNNING:
			case STARTING:
				try {
                    if (heating.getRuntime() >= heating.getMinRuntime()) {
                    	doStop(heating);
						onStop(heating);
                    }
				} catch (EnergyManagementException e) {
					logger.warn("Unable to stop heating \"{}\": {}", heating.getId(), e.getMessage());
				}
				break;
			default:
				logger.trace("Unable to stop already running heating: {}", heating.getId());
				break;
			}
		}
	}

	protected void doStop(HeatingService heating) throws EnergyManagementException {
		heating.stop();
	}

	protected void onStop(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected Value getStartValue(HeatingService heating) {
		return new DoubleValue(heating.getMinPower());
	}

}
