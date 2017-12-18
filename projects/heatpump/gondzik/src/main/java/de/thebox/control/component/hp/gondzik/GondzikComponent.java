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

@Component
public class GondzikComponent implements HeatPumpService, CabinetService {
	private final static Logger logger = LoggerFactory.getLogger(GondzikComponent.class);
	private final static String ID = "Gondzik";
	
	private ControlService control;
	private CabinetVentilation ventilation = null;
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;
		
		activateVentilation();
	}
	
	@Override
	public void reload() throws ComponentException {
		if (ventilation != null) {
			ventilation.deactivate();
		}
		activateVentilation();
	}
	
	private void activateVentilation() throws ComponentConfigException {
		try {
			Preferences configs = control.readComponentConfigs(ID);
			ventilation = new CabinetVentilation(control, configs.node(CabinetVentilationConst.VENTILATION_SECTION));

		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Gondzik configuration: {}", e.getMessage());
		}
	}
	
	@Override
	public void deactivate() {
		if (ventilation != null) {
			ventilation.deactivate();
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
	public void startHeating(Long timestamp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stopHeating(Long timestamp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void scheduleHeating(long start, long stop, double energy) {
		// TODO Auto-generated method stub
		
	}

}
