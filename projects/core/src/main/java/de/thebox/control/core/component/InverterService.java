package de.thebox.control.core.component;

public interface InverterService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.INVERTER;
	};

	public void setObjective(double value) throws ComponentException;

	public void resetObjective() throws ComponentException;

}
