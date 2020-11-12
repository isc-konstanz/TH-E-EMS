package org.the.ems.ctrl;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configurable;
import org.the.ems.core.data.Value;

public class ControlledInverter extends Configurable {
	private final static Logger logger = LoggerFactory.getLogger(ControlledInverter.class);

	public static class ControlledInverters extends LinkedHashMap<String, ControlledInverter> {
		private static final long serialVersionUID = -3866430817469938810L;

		public void add(ControlledInverter inverter) {
			put(inverter.getId(), inverter);
		}

		public void set(Value value) {
			for (ControlledInverter controlledInverter : values()) {
				// TODO: Implement verification, if the inverter is at its 
				// maximum capacity to react to setpoints or not
				controlledInverter.set(value);
			}
		}
	}

	private final InverterService inverter;

	private final ControlCallbacks control;

	protected ControlledInverter(ControlCallbacks callbacks, InverterService inverter) {
		this.control = callbacks;
		this.inverter = inverter;
	}

	public String getId() {
		return inverter.getId();
	}

	public InverterService getService() {
		return inverter;
	}

	public final void set(Value value) {
		doSet(value);
	}

	final void doSet(Value value) {
		try {
			control.onSet(this, value);
			this.onSet(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set heating \"{}\": {}", inverter.getId(), e.getMessage());
		}
	}

	protected void onSet(Value value) throws EnergyManagementException {
		inverter.set(value);
	}

}
