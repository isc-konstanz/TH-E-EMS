package de.thebox.control.opt;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.schedule.ControlSchedule;
import de.thebox.control.core.schedule.ScheduleListener;
import de.thebox.control.core.schedule.ScheduleService;

@Component
public class Optimization implements ScheduleService {

	@Override
	public ControlSchedule getOptimizationSchedule(ScheduleListener listener) {
		// TODO Auto-generated method stub

		return null;
	}

}
