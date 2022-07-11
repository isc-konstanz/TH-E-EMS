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
		private static final long serialVersionUID = 4407719774138591764L;

		public boolean hasChargableStorage() {
			return values().stream().anyMatch(c -> c.hasChargableStorage());
		}

		public boolean hasDischargableStorage() {
			return values().stream().anyMatch(c -> c.hasDischargableStorage());
		}

		public void set(Value value) {
			for (InverterControl controlledInverter : values()) {
				// TODO: Implement verification, if the inverter is at its 
				// maximum capacity to react to setpoints or not
				controlledInverter.set(value);
			}
		}

	}

	public interface InverterCallbacks extends ControlCallbacks {

		void onSet(InverterService component, Value value) throws EnergyManagementException;
	}

	protected InverterControl(InverterCallbacks callbacks, InverterService inverter) throws ComponentException {
		super(callbacks, inverter);
	}

	public boolean hasChargableStorage() {
		try {
			return component.getEnergyStorage().isChargable();
			
		} catch (ComponentException | InvalidValueException e) {
			logger.warn("Unable to retrieve chargable status of storage from inverter \"{}\": {}", 
					component.getId(), e.getMessage());
			return false;
		}
	}

	public boolean hasDischargableStorage() {
		try {
			return component.getEnergyStorage().isDischargable();
			
		} catch (ComponentException | InvalidValueException e) {
			logger.warn("Unable to retrieve chargable status of storage from inverter \"{}\": {}", 
					component.getId(), e.getMessage());
			return false;
		}
	}

	public final void set(Value value) {
		doSet(value);
	}

	void doSet(Value value) {
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
