package de.thebox.control.core.component;

import de.thebox.control.core.schedule.Schedule;
import de.thebox.control.core.schedule.Scheduler;

public abstract class ScheduleComponent extends ComponentImpl {
	protected final Scheduler scheduler;

	public ScheduleComponent() {
		scheduler = new Scheduler(getId());
	}

	public void schedule(Schedule schedule) throws ComponentException {
		// TODO: replace temporary placeholder
	}

}
