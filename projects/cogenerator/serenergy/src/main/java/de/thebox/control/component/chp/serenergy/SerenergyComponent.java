package de.thebox.control.component.chp.serenergy;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.CogeneratorService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;
import de.thebox.control.feature.circulation.pump.CirculationPump;
import de.thebox.control.feature.circulation.pump.CirculationPumpConst;

@Component
public class SerenergyComponent implements CogeneratorService {
	private final static Logger logger = LoggerFactory.getLogger(SerenergyComponent.class);
	private final static String ID = "Serenergy";

	private Preferences configs;

	private ControlService control;
	private CirculationPump circulation = null;

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
			configs = control.readComponentConfigs(ID);
			activateCirculation();
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Serenergy configuration: {}", e.getMessage());
		}
	}

	private void activateCirculation() throws ComponentConfigException {
		circulation = new CirculationPump(control, configs.node(CirculationPumpConst.CIRCULATION_SECTION));
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		activateCirculation();
	}

	@Override
	public void deactivate() {
		if (circulation != null) {
			circulation.deactivate();
		}
	}

	@Override
	public void startGeneration(double value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startGeneration(Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopGeneration(Long timestamp) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleGeneration(Schedule schedule) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
