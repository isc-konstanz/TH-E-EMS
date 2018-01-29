package de.thebox.control.core.schedule;

import de.thebox.control.core.component.ComponentService;

public interface ScheduleService extends ComponentService {

	public void registerScheduleListener(ScheduleListener listener);

	public void deregisterScheduleListener(ScheduleListener listener);

	public ControlSchedule getSchedule(ScheduleListener listener);

	public ControlSchedule getSchedule();

}
