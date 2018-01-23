package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface InverterService extends ComponentService {

	public void setObjective(Value value);

	public void resetObjective(Value value);

	public void scheduleObjective(Schedule schedule);

}
