package org.the.ems.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.RunState;
import org.the.ems.core.RunnableService;
import org.the.ems.core.data.Value;

public class ApplianceControl extends ComponentControl<RunnableService> {
	private final static Logger logger = LoggerFactory.getLogger(ApplianceControl.class);

	public static class ApplianceCollection extends ComponentCollection<ApplianceControl> {
		private static final long serialVersionUID = -1170124420619031062L;

		public void startAll(ComponentType... types) {
			for (ApplianceControl controlledAppliance : getAll(types)) {
				controlledAppliance.start();
			}
		}

		public void startFirst(ComponentType... types) {
			for (ApplianceControl controlledAppliance : getAll(types)) {
				if (controlledAppliance.isStartable()) {
					controlledAppliance.start();
					return;
				}
			}
		}

		public boolean hasStartable(ComponentType... types) {
			return getAll(types).stream().anyMatch(c -> c.isStartable());
		}

		public void stopAll(ComponentType... types) {
			for (ApplianceControl controlledAppliance : getAll(types)) {
				controlledAppliance.stop();
			}
		}

		public void stopFirst(ComponentType... types) {
			for (ApplianceControl controlledAppliance : getAll(types)) {
				if (controlledAppliance.isStoppable()) {
					controlledAppliance.stop();
					return;
				}
			}
		}

		public boolean hasStoppable(ComponentType... types) {
			return getAll(types).stream().anyMatch(c -> c.isStoppable());
		}

	}

	public interface ApplianceCallbacks extends ControlCallbacks {

		void onSet(RunnableService component, Value value) throws EnergyManagementException;

		void onStart(RunnableService component, Value value) throws EnergyManagementException;

		void onStop(RunnableService component) throws EnergyManagementException;
	}

	protected ApplianceControl(ApplianceCallbacks callbacks, RunnableService appliance) throws ComponentException {
		super(callbacks, appliance);
	}

	public final void start() {
		if (isStartable()) {
			doStart();
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Unable to start alreading running or briefly idling appliance: {}", component.getId());
        }
	}

	protected final void doStart() {
		Value value = component.getStartValue();
		try {
			this.onStart(value);
			((ApplianceCallbacks) callbacks).onStart(component, value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to start appliance \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onStart(Value value) throws EnergyManagementException {
		component.start(value);
	}

	public boolean isStartable() {
		RunState state = component.getState();
		switch(state) {
		case STANDBY:
		case STOPPING:
            if (component.getIdletime() >= component.getMinIdletime()) {
    			return true;
            }
		default:
			break;
		}
		return false;
	}

	public final void set(Value value) {
		RunState state = component.getState();
		switch(state) {
		case STANDBY:
		case STOPPING:
			doSet(value);
			break;
		default:
			if (logger.isTraceEnabled()) {
				logger.trace("Unable to set idling appliance: {}", component.getId());
	        }
			break;
		}
	}

	protected final void doSet(Value value) {
		try {
			this.onSet(value);
			((ApplianceCallbacks) callbacks).onSet(component, value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set appliance \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onSet(Value value) throws EnergyManagementException {
		component.set(value);
	}

	public final void stop() {
		if (isStoppable()) {
			doStop();
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Unable to start alreading in standby or briefly running appliance: {}", component.getId());
        }
	}

	protected final void doStop() {
		try {
			this.onStop();
			((ApplianceCallbacks) callbacks).onStop(component);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to stop appliance \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onStop() throws EnergyManagementException {
		component.stop();
	}

	public boolean isStoppable() {
		RunState state = component.getState();
		switch(state) {
		case RUNNING:
		case STARTING:
            if (component.getRuntime() >= component.getMinRuntime()) {
            	return true;
            }
		default:
			break;
		}
		return false;
	}

}
