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

	protected final void onControlHeating() {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				switch(heating.getState()) {
				case STANDBY:
				case STOPPING:
					if (checkStart(heating)) {
						doStart(heating);
						onStart(heating);
					}
					break;
				case RUNNING:
				case STARTING:
					if (checkStop(heating) && heating.getRuntime() >= heating.getMinRuntime()) {
						doStop(heating);
						onStop(heating);
					}
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to control heating \"{}\": {}", key, e.getMessage());
			}
		}
	}

	protected boolean checkStart(HeatingService heating) {
		// Default implementation to be overridden
		return true;
	}

	protected void doStart(HeatingService heating) throws EnergyManagementException {
		heating.start(getValue(heating));
	}

	protected Value getValue(HeatingService heating) {
		return new DoubleValue(heating.getMaxPower());
	}

	protected void onStart(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	protected boolean checkStop(HeatingService heating) {
		// Default implementation to be overridden
		return true;
	}

	protected void doStop(HeatingService heating) throws EnergyManagementException {
		heating.stop();
	}

	protected void onStop(HeatingService heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

}
