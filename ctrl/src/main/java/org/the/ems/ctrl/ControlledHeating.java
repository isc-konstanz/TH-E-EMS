package org.the.ems.ctrl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.HeatingState;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public class ControlledHeating {
	private final static Logger logger = LoggerFactory.getLogger(ControlledHeating.class);

	public static class ControlledHeatings extends LinkedHashMap<String, ControlledHeating> {
		private static final long serialVersionUID = -1170124420619031062L;

		public void add(ControlledHeating heating) {
			put(heating.getId(), heating);
		}

		public List<ControlledHeating> getAll(ComponentType... types) {
			List<ControlledHeating> components = new ArrayList<ControlledHeating>();
			for (ComponentType type : types) {
				for (ControlledHeating heating : values()) {
					if (heating.getType() == type) {
						components.add(heating);
					}
				}
			}
			return components;
		}

		public void startAll(ComponentType... types) {
			for (ControlledHeating controlledHeating : getAll(types)) {
				controlledHeating.start();
			}
		}

		public void startFirst(ComponentType... types) {
			for (ControlledHeating controlledHeating : getAll(types)) {
				if (controlledHeating.isStartable()) {
					controlledHeating.start();
					return;
				}
			}
		}

		public boolean hasStartable(ComponentType... types) {
			return getAll(types).stream().anyMatch(c -> c.isStartable());
		}

		public void stopAll(ComponentType... types) {
			for (ControlledHeating controlledHeating : getAll(types)) {
				controlledHeating.stop();
			}
		}

		public void stopFirst(ComponentType... types) {
			for (ControlledHeating controlledHeating : getAll(types)) {
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

	private final HeatingService heating;

	private final ControlCallbacks control;

	protected ControlledHeating(ControlCallbacks callbacks, HeatingService heating) {
		this.control = callbacks;
		this.heating = heating;
	}

	public String getId() {
		return heating.getId();
	}

	public ComponentType getType() {
		return heating.getType();
	}

	public HeatingService getService() {
		return heating;
	}

	protected Value getStartValue() {
		return new DoubleValue(heating.getMinPower());
	}

	public final void start() {
		if (isStartable()) {
			doStart();
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Unable to start alreading running or briefly idling heating: {}", heating.getId());
        }
	}

	protected final void doStart() {
		Value value = getStartValue();
		try {
			control.onStart(this, value);
			this.onStart(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to start heating \"{}\": {}", heating.getId(), e.getMessage());
		}
	}

	protected void onStart(Value value) throws EnergyManagementException {
		heating.start(value);
	}

	public boolean isStartable() {
		HeatingState state = heating.getState();
		switch(state) {
		case STANDBY:
		case STOPPING:
            if (heating.getIdletime() >= heating.getMinIdletime()) {
    			return true;
            }
		default:
			break;
		}
		return false;
	}

	public final void set(Value value) {
		HeatingState state = heating.getState();
		switch(state) {
		case STANDBY:
		case STOPPING:
			doSet(value);
			break;
		default:
			if (logger.isTraceEnabled()) {
				logger.trace("Unable to set idling heating: {}", heating.getId());
	        }
			break;
		}
	}

	protected final void doSet(Value value) {
		try {
			control.onSet(this, value);
			this.onSet(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set heating \"{}\": {}", heating.getId(), e.getMessage());
		}
	}

	protected void onSet(Value value) throws EnergyManagementException {
		heating.set(value);
	}

	public final void stop() {
		if (isStoppable()) {
			doStop();
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Unable to start alreading in standby or briefly running heating: {}", heating.getId());
        }
	}

	protected final void doStop() {
		try {
			control.onStop(this);
			this.onStop();
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to stop heating \"{}\": {}", heating.getId(), e.getMessage());
		}
	}

	protected void onStop() throws EnergyManagementException {
		heating.stop();
	}

	public boolean isStoppable() {
		HeatingState state = heating.getState();
		switch(state) {
		case RUNNING:
		case STARTING:
            if (heating.getRuntime() >= heating.getMinRuntime()) {
            	return true;
            }
		default:
			break;
		}
		return false;
	}

}
