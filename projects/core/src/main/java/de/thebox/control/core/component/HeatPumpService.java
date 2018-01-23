package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface HeatPumpService extends ComponentService {

	public void startHeating(Value value);

	public void stopHeating(Value value);

	public void scheduleHeating(Schedule schedule);

}
