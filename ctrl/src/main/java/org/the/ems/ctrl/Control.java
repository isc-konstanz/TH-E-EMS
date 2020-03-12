package org.the.ems.ctrl;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.HeatingState;
import org.the.ems.core.HeatingService;
import org.the.ems.core.UnknownComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public abstract class Control extends Component {
	private final static Logger logger = LoggerFactory.getLogger(Control.class);

	public final static String PID = "org.the.ems.ctrl";

	protected ComponentService component;

	@Configuration("component")
	protected String key;

	@Override
	public ComponentType getType() {
		return ComponentType.CONTROL;
	}

	@Override
	public void onActivate(Configurations configs, BundleContext context) throws ComponentException {
		super.onActivate(configs, context);
		logger.info("Activating TH-E EMS {}: {}", this.getClass().getSimpleName(), getId());
		try {
			component = context.getService(context.getServiceReference(EnergyManagementService.class))
					.getComponent(key);
			
		} catch (UnknownComponentException e) {
			throw new ConfigurationException("Unable to find controllable component: " + key);
		}
	}

	public final void start() {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				HeatingState state = heating.getState();
				switch(state) {
				case STANDBY:
				case STOPPING:
					doStart(heating);
					onStart(heating);
					break;
				default:
					logger.trace("Unable to start already running heating: {}", key);
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to start heating \"{}\": {}", key, e.getMessage());
			}
		}
	}

	protected void doStart(HeatingService heating) throws EnergyManagementException {
		heating.start(getValue(heating));
	}

	protected void onStart(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	public final void stop() {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				HeatingState state = heating.getState();
				switch(state) {
				case RUNNING:
				case STARTING:
                    if (heating.getRuntime() >= heating.getMinRuntime()) {
                    	doStop(heating);
						onStop(heating);
                    }
					break;
				default:
					logger.trace("Unable to stop already running heating: {}", key);
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to control heating \"{}\": {}", key, e.getMessage());
			}
		}
	}

	protected void doStop(HeatingService heating) throws EnergyManagementException {
		heating.stop();
	}

	protected void onStop(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected Value getValue(HeatingService heating) {
		return new DoubleValue(heating.getMaxPower());
	}

}
