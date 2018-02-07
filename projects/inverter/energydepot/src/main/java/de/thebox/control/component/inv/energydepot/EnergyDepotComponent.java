package de.thebox.control.component.inv.energydepot;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.inv.energydepot.consumption.Consumption;
import de.thebox.control.component.inv.energydepot.objective.Objective;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

@Component
public class EnergyDepotComponent implements InverterService {
	private final static Logger logger = LoggerFactory.getLogger(EnergyDepotComponent.class);
	private final static String ID = "EnergyDepot";

	private ControlService control;
	private Objective objective;
	private Consumption consumption;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;
		activateComponent();
	}

	private void activateComponent() throws ComponentException {
		try {
			Preferences config = control.readComponentConfigs(ID);
			activateConsumption(config);
			activateObjective(config);
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Energy Depot configuration: {}", e.getMessage());
		}
	}

	private void activateConsumption(Preferences config) throws ComponentException {
		consumption = new Consumption(control, config);
	}

	private void activateObjective(Preferences config) throws ComponentException {
		objective = new Objective(control, consumption, config);
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		activateComponent();
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
	public void setObjective(Value value) throws ComponentException {
		// TODO Auto-generated method stub
	}

	@Override
	public void resetObjective(Long timestamp) throws ComponentException {
		if (timestamp != null) {
			// TODO
		}
		else {
			objective.reset();
		}
	}

	@Override
	public void schedule(Schedule schedule) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
