package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface InverterService extends ComponentService {

	public void setObjective(double value) throws ComponentException;

	public void setObjective(Value value) throws ComponentException;

	public void resetObjective(Long timestamp) throws ComponentException;

	public void scheduleObjective(Schedule schedule) throws ComponentException;

}
