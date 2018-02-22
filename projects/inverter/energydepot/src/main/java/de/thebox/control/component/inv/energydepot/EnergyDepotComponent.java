package de.thebox.control.component.inv.energydepot;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.component.inv.energydepot.consumption.Consumption;
import de.thebox.control.component.inv.energydepot.objective.Objective;
import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.component.ScheduleComponent;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

@Component
public class EnergyDepotComponent extends ScheduleComponent implements InverterService {
	private final static String ID = "EnergyDepot";

	private Consumption consumption;
	private Objective objective;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences config) throws ControlException {
		consumption = new Consumption(control, config);
		objective = new Objective(control, consumption, config);
	}

	@Override
	public void deactivate() {
		if (objective != null) {
			objective.deactivate(consumption);
		}
		if (consumption != null) {
			consumption.deactivate();
		}
	}

	@Override
	public void setObjective(double value) throws ComponentException {
		objective.set(value);
	}

	@Override
	public void resetObjective() throws ComponentException {
		objective.reset();
	}

	@Override
	public void schedule(Schedule schedule) throws ComponentException {
		Value value = schedule.pollFirst();
		if (value != null) {
			objective.setSetpoint(value);
		}
	}

}
