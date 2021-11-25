package org.the.ems.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.RunState;
import org.the.ems.core.data.Value;

public class HeatingControl extends ComponentControl<HeatingService> {
	private final static Logger logger = LoggerFactory.getLogger(HeatingControl.class);

	public static class HeatingCollection extends ComponentCollection<HeatingControl> {
		private static final long serialVersionUID = -1170124420619031062L;

		public void startAll(ComponentType... types) {
			for (HeatingControl controlledHeating : getAll(types)) {
				controlledHeating.start();
			}
		}

		public void startFirst(ComponentType... types) {
			for (HeatingControl controlledHeating : getAll(types)) {
				if (controlledHeating.isStartable()) {
					controlledHeating.start();
					return;
				}
			}
		}

		public boolean hasStartable(ComponentType... types) {
			return getAll(types).stream().anyMatch(c -> c.isStartable());
		}

		public double getStartableMinPower(ComponentType... types) {
			return getAll(types).stream().mapToDouble(c -> c.getComponent().getMinPower()).min().orElse(0);
		}

		public double getStartableMaxPower(ComponentType... types) {
			return getAll(types).stream().mapToDouble(c -> c.getComponent().getMaxPower()).max().orElse(Double.MAX_VALUE);
		}

		public void stopAll(ComponentType... types) {
			for (HeatingControl controlledHeating : getAll(types)) {
				controlledHeating.stop();
			}
		}

		public void stopFirst(ComponentType... types) {
			for (HeatingControl controlledHeating : getAll(types)) {
				if (controlledHeating.isStoppable()) {
					controlledHeating.stop();
					return;
				}
			}
		}

		public boolean hasStoppable(ComponentType... types) {
			return getAll(types).stream().anyMatch(c -> c.isStoppable());
		}

	}

	public interface HeatingCallbacks extends ControlCallbacks {

		void onSet(HeatingService component, Value value) throws EnergyManagementException;

		void onStart(HeatingService component, Value value) throws EnergyManagementException;

		void onStop(HeatingService component) throws EnergyManagementException;
	}

	protected HeatingControl(HeatingCallbacks callbacks, HeatingService heating) throws ComponentException {
		super(callbacks, heating);
	}

	public final void start() {
		if (isStartable()) {
			doStart();
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Unable to start alreading running or briefly idling heating: {}", component.getId());
        }
	}

	void doStart() {
		Value value = component.getStartValue();
		try {
			this.onStart(value);
			((HeatingCallbacks) callbacks).onStart(component, value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to start heating \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onStart(Value value) throws EnergyManagementException {
		component.start(value);
	}

	public boolean isStartable() {
		return component.isStartable();
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
				logger.trace("Unable to set idling heating: {}", component.getId());
	        }
			break;
		}
	}

	void doSet(Value value) {
		try {
			this.onSet(value);
			((HeatingCallbacks) callbacks).onSet(component, value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set heating \"{}\": {}", component.getId(), e.getMessage());
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
			logger.trace("Unable to start alreading in standby or briefly running heating: {}", component.getId());
        }
	}

	void doStop() {
		try {
			this.onStop();
			((HeatingCallbacks) callbacks).onStop(component);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to stop heating \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onStop() throws EnergyManagementException {
		component.stop();
	}

	public boolean isStoppable() {
		return component.isStoppable();
	}

}
