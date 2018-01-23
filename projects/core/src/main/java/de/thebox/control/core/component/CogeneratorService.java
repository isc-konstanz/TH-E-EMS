package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface CogeneratorService extends ComponentService {

	public void startGeneration(Value value);

	public void stopGeneration(Value value);

	public void scheduleGeneration(Schedule schedule);

}
