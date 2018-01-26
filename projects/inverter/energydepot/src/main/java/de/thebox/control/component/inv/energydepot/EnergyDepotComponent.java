package de.thebox.control.component.inv.energydepot;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.inv.energydepot.objective.ExternalObjective;
import de.thebox.control.component.inv.energydepot.objective.ExternalObjectiveConst;
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

	private Preferences configs;

	private ControlService control;
	private ExternalObjective externalObjective;

	private String objectiveValue;

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
			configs = control.readComponentConfigs(ID);
			
			Preferences configs = this.configs.node(EnergyDepotConst.INVERTER_SECTION);
			objectiveValue = configs.get(EnergyDepotConst.OBJECTIVE_KEY, null);
			activateExternalObjective();
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Energy Depot configuration: {}", e.getMessage());
		}
	}

	private void activateExternalObjective() throws ComponentException {
		try {
			if (configs.nodeExists(ExternalObjectiveConst.EXTERNAL_SECTION)) {
				Preferences emoncms = null;
				if (configs.nodeExists(ExternalObjectiveConst.EMONCMS_SECTION)) {
					emoncms = configs.node(ExternalObjectiveConst.EMONCMS_SECTION);
				}
				externalObjective = new ExternalObjective(this, control, configs.node(ExternalObjectiveConst.EXTERNAL_SECTION), emoncms);
			}
		} catch (BackingStoreException e) {
			throw new ComponentConfigException("Error while reading Energy Depot external configuration: " + e.getMessage());
		}
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		activateComponent();
	}

	@Override
	public void deactivate() {
		if (externalObjective != null) {
			externalObjective.deactivate();
		}
	}

	@Override
	public boolean setObjective(double value) throws ComponentException {
		if (value <= EnergyDepotConst.OBJECTIVE_MAX && value >= EnergyDepotConst.OBJECTIVE_MIN) {
			return control.writeValue(objectiveValue, new DoubleValue(value));
		}
		throw new ComponentException("Inverter objective out of bounds: " + value);
	}

	@Override
	public boolean setObjective(Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
		return false;
	}

	@Override
	public boolean resetObjective(Long timestamp) throws ComponentException {
		if (timestamp != null) {
			return false;
		}
		else {
			return control.writeValue(objectiveValue, new DoubleValue(EnergyDepotConst.OBJECTIVE_DEFAULT));
		}
	}

	@Override
	public void scheduleObjective(Schedule schedule) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
