package de.thebox.control.component.chp.serenergy;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.chp.serenergy.circulation.CirculationPump;
import de.thebox.control.component.chp.serenergy.circulation.CirculationPumpConst;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.CogeneratorService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;

@Component
public class SerenergyComponent implements CogeneratorService {
	private final static Logger logger = LoggerFactory.getLogger(SerenergyComponent.class);
	private final static String ID = "Serenergy";
	
	private ControlService control;
	private CirculationPump circulation = null;
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public void activate(ControlService context) throws ComponentException {
		this.control = context;
		
		activateCirculation();
	}
	
	@Override
	public void reload() throws ComponentException {
		if (circulation != null) {
			circulation.deactivate();
		}
		activateCirculation();
	}
	
	private void activateCirculation() throws ComponentConfigException {
		try {
			Preferences configs = control.readComponentConfigs(ID);
			circulation = new CirculationPump(control, configs.node(CirculationPumpConst.CIRCULATION_SECTION));
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			logger.error("Error while reading Serenergy configuration: {}", e.getMessage());
		}
	}
	
	@Override
	public void deactivate() {
		if (circulation != null) {
			circulation.deactivate();
		}
	}
	
	@Override
	public void startGeneration(Long timestamp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stopGeneration(Long timestamp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void scheduleGeneration(long start, long stop, double energy) {
		// TODO Auto-generated method stub
		
	}

}
