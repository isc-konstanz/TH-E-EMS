package de.thebox.control.component.inv.energydepot;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

@Component
public class EnergyDepotComponent implements InverterService {
	private final static Logger logger = LoggerFactory.getLogger(EnergyDepotComponent.class);
	private final static String ID = "EnergyDepot";

	private String objectiveValue;

	private ControlService control;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;
		activateComponent();
	}

	private void activateComponent() throws ComponentConfigException {
		try {
			Preferences configs = control.readComponentConfigs(ID).node(EnergyDepotConst.INVERTER_SECTION);
			objectiveValue = configs.get(EnergyDepotConst.OBJECTIVE_KEY, null);
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Energy Depot configuration: {}", e.getMessage());
		}
	}

	@Override
	public void reload() throws ComponentException {
		activateComponent();
	}

	@Override
	public void deactivate() {
		// Not implemented for this component
	}

	@Override
	public void setObjective(double value) throws ComponentException {
		if (value <= EnergyDepotConst.OBJECTIVE_MAX && value >= EnergyDepotConst.OBJECTIVE_MIN) {
			control.writeValue(objectiveValue, new DoubleValue(value));
		}
		throw new ComponentException("Inverter objective out of bounds: " + value);
	}

	@Override
	public void setObjective(Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetObjective(Long timestamp) throws ComponentException {
		if (timestamp != null) {
			
		}
		else {
			control.writeValue(objectiveValue, new DoubleValue(EnergyDepotConst.OBJECTIVE_DEFAULT));
		}
	}

	@Override
	public void scheduleObjective(Schedule schedule) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
