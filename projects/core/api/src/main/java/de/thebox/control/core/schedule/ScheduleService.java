package de.thebox.control.core.schedule;

public interface ScheduleService {

	public void registerScheduleListener(ScheduleListener listener);

	public void deregisterScheduleListener(ScheduleListener listener);

	public ControlSchedule getSchedule(ScheduleListener listener);

	public ControlSchedule getSchedule();

}
