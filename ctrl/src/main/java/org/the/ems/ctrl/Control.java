package org.the.ems.ctrl;

import org.osgi.framework.BundleContext;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.UnknownComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public abstract class Control extends Component {
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
		try {
			component = context.getService(context.getServiceReference(EnergyManagementService.class))
					.getComponent(key);
		} catch (UnknownComponentException e) {
			throw new ConfigurationException("Unable to find controllable component: " + key);
		}
	}

}
