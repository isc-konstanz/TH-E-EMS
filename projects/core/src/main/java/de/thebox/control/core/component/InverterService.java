package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;

public interface InverterService extends ComponentScheduleService {

	@Override
	public default Component getType() {
		return Component.COGENERATOR;
	};

	public void setObjective(double value) throws ComponentException;

	public void setObjective(Value value) throws ComponentException;

	public void resetObjective(Long timestamp) throws ComponentException;

}
