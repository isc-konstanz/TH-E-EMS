package de.thebox.control.component.inv.energydepot;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

@Component
public class EnergyDepotComponent implements InverterService {
	private final static Logger logger = LoggerFactory.getLogger(EnergyDepotComponent.class);
	private final static String ID = "EnergyDepot";

	private Preferences configs;

	private ControlService control;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;

		try {
			configs = control.readComponentConfigs(ID);
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Energy Depot configuration: {}", e.getMessage());
		}
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
	}

	@Override
	public void deactivate() {

	}

	@Override
	public void setObjective(Value value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetObjective(Value value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleObjective(Schedule schedule) {
		// TODO Auto-generated method stub
		
	}

}
