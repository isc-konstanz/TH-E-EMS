package org.the.ems.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

public class InverterControl extends ComponentControl<InverterService> {
	private final static Logger logger = LoggerFactory.getLogger(InverterControl.class);

	public static class InverterCollection extends ComponentCollection<InverterControl> {
		private static final long serialVersionUID = -3866430817469938810L;

		public void set(Value value) {
			for (InverterControl controlledInverter : values()) {
				// TODO: Implement verification, if the inverter is at its 
				// maximum capacity to react to setpoints or not
				controlledInverter.set(value);
			}
		}
		
		public boolean hasSufficientCapacity() {
			return values().stream().anyMatch(c -> c.isAbleToExport());
		}
		
	}

	public interface InverterCallbacks extends ControlCallbacks {

		void onSet(InverterService component, Value value) throws EnergyManagementException;
	}

	protected InverterControl(InverterCallbacks callbacks, InverterService inverter) throws ComponentException {
		super(callbacks, inverter);
	}
	
	public boolean isAbleToExport() {
		try {
			return component.getStorage().hasMinStateOfCharge();
		} catch (ComponentException | InvalidValueException e) {
			logger.warn("Unable to retrieve minimum SOC \"{}\": {}", component.getId(), e.getMessage());
			return false;
		}
	}

	public final void set(Value value) {
		doSet(value);
	}

	final void doSet(Value value) {
		try {
			this.onSet(value);
			((InverterCallbacks) callbacks).onSet(component, value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set inverter \"{}\": {}", component.getId(), e.getMessage());
		}
	}

	protected void onSet(Value value) throws EnergyManagementException {
		component.set(value);
	}

}
