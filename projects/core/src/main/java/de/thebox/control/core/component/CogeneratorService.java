package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface CogeneratorService extends ComponentService {

	public void startGeneration(double value) throws ComponentException;

	public void startGeneration(Value value) throws ComponentException;

	public void stopGeneration(Long timestamp) throws ComponentException;

	public void scheduleGeneration(Schedule schedule) throws ComponentException;

}
