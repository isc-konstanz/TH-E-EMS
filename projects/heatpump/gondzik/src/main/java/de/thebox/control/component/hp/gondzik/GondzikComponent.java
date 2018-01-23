package de.thebox.control.component.hp.gondzik;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.hp.gondzik.circulation.CirculationPump;
import de.thebox.control.component.hp.gondzik.circulation.CirculationPumpConst;
import de.thebox.control.component.hp.gondzik.ventilation.CabinetVentilation;
import de.thebox.control.component.hp.gondzik.ventilation.CabinetVentilationConst;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.CabinetService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.HeatPumpService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

@Component
public class GondzikComponent implements HeatPumpService, CabinetService {
	private final static Logger logger = LoggerFactory.getLogger(GondzikComponent.class);
	private final static String ID = "Gondzik";

	private Preferences configs;

	private ControlService control;
	private CabinetVentilation ventilation = null;
	private CirculationPump circulation = null;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;

		try {
			configs = control.readComponentConfigs(ID);
			activateVentilation();
			activateCirculation();
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Serenergy configuration: {}", e.getMessage());
		}
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		
		activateVentilation();
		activateVentilation();
	}

	private void activateVentilation() throws ComponentConfigException {
		ventilation = new CabinetVentilation(control, configs.node(CabinetVentilationConst.VENTILATION_SECTION));
	}

	private void activateCirculation() throws ComponentConfigException {
		circulation = new CirculationPump(control, configs.node(CirculationPumpConst.CIRCULATION_SECTION));
	}

	@Override
	public void deactivate() {
		if (ventilation != null) {
			ventilation.deactivate();
		}
		if (circulation != null) {
			circulation.deactivate();
		}
	}

	@Override
	public void startVentilation(Long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopVentilation(Long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startHeating(Value value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopHeating(Value value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleHeating(Schedule schedule) {
		// TODO Auto-generated method stub
		
	}

}
