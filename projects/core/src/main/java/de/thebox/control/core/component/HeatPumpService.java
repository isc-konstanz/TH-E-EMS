package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;

public interface HeatPumpService extends ComponentScheduleService {

	@Override
	public default Component getType() {
		return Component.HEATPUMP;
	};

	public void startHeating(double value) throws ComponentException;

	public void startHeating(Value value) throws ComponentException;

	public void stopHeating(Long timestamp) throws ComponentException;

}
