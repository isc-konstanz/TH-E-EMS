package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface HeatPumpService extends ComponentService {

	public void startHeating(double value) throws ComponentException;

	public void startHeating(Value value) throws ComponentException;

	public void stopHeating(Long timestamp) throws ComponentException;

	public void scheduleHeating(Schedule schedule) throws ComponentException;

}
