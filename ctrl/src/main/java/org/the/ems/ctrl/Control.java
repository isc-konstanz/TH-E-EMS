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
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationCollection.BooleanCollection;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.ctrl.HeatingControl.HeatingCallbacks;
import org.the.ems.ctrl.HeatingControl.HeatingCollection;
import org.the.ems.ctrl.InverterControl.InverterCallbacks;
import org.the.ems.ctrl.InverterControl.InverterCollection;

public abstract class Control extends Component implements InverterCallbacks, HeatingCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(Control.class);

	public final static String PID = "org.the.ems.ctrl";

	protected final InverterCollection inverters = new InverterCollection();
	protected final HeatingCollection heatings = new HeatingCollection();

	@Configuration(value="*_enabled", mandatory=false)
	private BooleanCollection enabled;

	@Configuration(value="mode", mandatory=false)
	protected ControlMode mode;

	@Override
	public ControlMode getMode() {
		return mode;
	}

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
			if (isEnabled(component)) {
				InverterControl inverter = doCreate((InverterService) component);
				inverters.add(inverter);
				onActivate(inverter);
				onActivate(component);
			}
		}
		for (ComponentService component : manager.getComponents(
				ComponentType.HEAT_PUMP,
				ComponentType.HEATING_ROD,
				ComponentType.COMBINED_HEAT_POWER)) {
			if (isEnabled(component)) {
				HeatingControl heating = doCreate((HeatingService) component);
				heatings.add(heating);
				onActivate(heating);
				onActivate(component);
			}
		}
	}

	protected InverterControl doCreate(InverterService service) throws ComponentException {
		InverterControl inverter = new InverterControl(this, service);
		this.onCreate(inverter);
		return inverter;
	}

	protected HeatingControl doCreate(HeatingService service) throws ComponentException {
		HeatingControl heating = new HeatingControl(this, service);
		this.onCreate(heating);
		return heating;
	}

	protected void onCreate(InverterControl inverter) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onCreate(HeatingControl inverter) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(InverterControl inverter) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(HeatingControl heating) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onActivate(ComponentService component) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	public void onStart(HeatingService heating, StartSettings settings) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onStop(HeatingService heating, StopSettings settings) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onSet(InverterService inverter, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	@Override
	public void onSet(HeatingService heating, Value value) throws EnergyManagementException {
		// Default implementation to be overridden
	}

	private boolean isEnabled(ComponentService component) {
		String componentId = component.getId();
		if (enabled.contains(componentId+"_enabled") && 
				!enabled.get(componentId+"_enabled")) {
			
			logger.info("Component configurations disabled: {}", componentId);
			return false;
		}
		return true;
	}

}
