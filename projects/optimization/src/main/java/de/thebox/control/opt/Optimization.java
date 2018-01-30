package de.thebox.control.opt;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.schedule.ControlSchedule;
import de.thebox.control.core.schedule.ScheduleListener;
import de.thebox.control.core.schedule.ScheduleService;

@Component
public class Optimization implements ScheduleService {
	private final static Logger logger = LoggerFactory.getLogger(Optimization.class);
	private final static String ID = "MPC";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(ControlService control) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reload() throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerScheduleListener(ScheduleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deregisterScheduleListener(ScheduleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ControlSchedule getSchedule(ScheduleListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ControlSchedule getSchedule() {
		// TODO Auto-generated method stub
		return null;
	}

}
