package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;

public interface CogeneratorService extends ComponentScheduleService {

	@Override
	public default Component getType() {
		return Component.COGENERATOR;
	};

	public void startGeneration(double value) throws ComponentException;

	public void startGeneration(Value value) throws ComponentException;

	public void stopGeneration(Long timestamp) throws ComponentException;

}
