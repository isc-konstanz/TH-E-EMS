package de.thebox.control.core.component;

import de.thebox.control.core.schedule.Schedule;

public interface ComponentScheduleService extends ComponentService {

	public default Component getType() {
		return Component.NONE;
	};

	public void schedule(Schedule schedule) throws ComponentException;

}
