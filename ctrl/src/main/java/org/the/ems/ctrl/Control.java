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
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.ctrl.ControlledHeating.ControlledHeatings;
import org.the.ems.ctrl.ControlledInverter.ControlledInverters;

public abstract class Control extends Component implements ControlCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(Control.class);

	public final static String PID = "org.the.ems.ctrl";

	protected final ControlledInverters inverters = new ControlledInverters();
	protected final ControlledHeatings heatings = new ControlledHeatings();

	@Override
	public ComponentType getType() {
		return ComponentType.CONTROL;
	}

	@Override
	public void onActivate(BundleContext context, Configurations configs) throws ComponentException {
		super.onActivate(context, configs);
		logger.info("Activating TH-E EMS {}: {}", this.getClass().getSimpleName(), getId());
		
		EnergyManagementService manager = context.getService(context.getServiceReference(EnergyManagementService.class));
		for (ComponentService component : manager.getComponents(
				ComponentType.INVERTER)) {
			ControlledInverter inverter = 
					onCreate((InverterService) component);
			this.onActivate(inverter);
			inverters.add(inverter);
		}
		for (ComponentService component : manager.getComponents(
				ComponentType.HEAT_PUMP,
				ComponentType.HEATING_ROD,
				ComponentType.COMBINED_HEAT_POWER)) {
			ControlledHeating heating = 
					onCreate((HeatingService) component);
			this.onActivate(heating);
			heatings.add(heating);
		}
	}

	protected ControlledInverter onCreate(InverterService inverter) throws ComponentException {
		return new ControlledInverter(this, inverter);
	}

	protected ControlledHeating onCreate(HeatingService heating) throws ComponentException {
		return new ControlledHeating(this, heating);
	}

	protected void onActivate(ControlledInverter inverter) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(ControlledHeating heating) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public void onStart(ControlledHeating heating, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onStop(ControlledHeating heating) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onSet(ControlledHeating heating, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onSet(ControlledInverter inverter, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

}
