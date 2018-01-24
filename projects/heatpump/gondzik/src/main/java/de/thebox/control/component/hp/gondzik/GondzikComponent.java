package de.thebox.control.component.hp.gondzik;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.hp.gondzik.ventilation.CabinetVentilation;
import de.thebox.control.component.hp.gondzik.ventilation.CabinetVentilationConst;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.CabinetService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.HeatPumpService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;
import de.thebox.control.feature.circulation.pump.CirculationPump;
import de.thebox.control.feature.circulation.pump.CirculationPumpConst;

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
		activateComponent();
	}

	private void activateComponent() throws ComponentConfigException {
		try {
			configs = control.readComponentConfigs(ID);
			activateVentilation();
			activateCirculation();
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Gondzik configuration: {}", e.getMessage());
		}
	}

	private void activateVentilation() throws ComponentConfigException {
		ventilation = new CabinetVentilation(control, configs.node(CabinetVentilationConst.VENTILATION_SECTION));
	}

	private void activateCirculation() throws ComponentConfigException {
		circulation = new CirculationPump(control, configs.node(CirculationPumpConst.CIRCULATION_SECTION));
	}

	@Override
	public void reload() throws ComponentException {
		deactivate();
		activateComponent();
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
	public void startVentilation(Long timestamp) throws ComponentException {
		// TODO Schedule by timestamp
		
		ventilation.start();
	}

	@Override
	public void stopVentilation(Long timestamp) throws ComponentException {
		// TODO Schedule by timestamp
		
		ventilation.stop();
	}

	@Override
	public void startHeating(double value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startHeating(Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopHeating(Long timestamp) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleHeating(Schedule schedule) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
