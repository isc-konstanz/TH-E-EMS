package de.thebox.control.component.inv.energydepot;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.component.inv.energydepot.consumption.Consumption;
import de.thebox.control.component.inv.energydepot.external.External;
import de.thebox.control.component.inv.energydepot.external.ExternalConfig;
import de.thebox.control.component.inv.energydepot.objective.Objective;
import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.component.ScheduleComponent;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;
import de.thebox.control.feature.emoncms.EmoncmsConfig;

@Component
public class EnergyDepotComponent extends ScheduleComponent implements InverterService {
	private final static String ID = "EnergyDepot";

	private External external;
	private Consumption consumption;
	private Objective objective;

	private volatile boolean maintenance = false;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences config) throws ControlException {
		try {
			if (config.nodeExists(ExternalConfig.SECTION) && config.nodeExists(EmoncmsConfig.SECTION)) {
				external = new External(control, config);
			}
			else {
				external = null;
			}
		} catch (BackingStoreException e) {
			throw new ConfigurationException("Error while reading configuration: " + e.getMessage());
		}
		consumption = new Consumption(control, config);
		objective = new Objective(control, external, consumption, config);
	}

	@Override
	public void deactivate() {
		if (objective != null) {
			objective.deactivate(consumption);
		}
		if (consumption != null) {
			consumption.deactivate();
		}
		if (external != null) {
			external.deactivate();
		}
	}

	@Override
	protected void maintenance(boolean enabled) throws ControlException {
		if (external != null) {
			external.setEnabled(!enabled);
		}
		maintenance = enabled;
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
		if (value != null && !maintenance) {
			objective.setSetpoint(value);
		}
	}

}
