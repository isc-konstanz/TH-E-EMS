package de.thebox.control.component.hp.gondzik;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.component.hp.gondzik.ventilation.CabinetVentilation;
import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.CabinetService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.HeatPumpService;
import de.thebox.control.core.component.ScheduleComponent;
import de.thebox.control.feature.circulation.Circulation;

@Component
public class GondzikComponent extends ScheduleComponent implements CabinetService, HeatPumpService {
	private final static String ID = "Gondzik";

	private CabinetVentilation ventilation;
	private Circulation circulation;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences config) throws ControlException {
		ventilation = new CabinetVentilation(control, config);
		circulation = new Circulation(control, config);
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
	public void startVentilation() throws ComponentException {
		ventilation.start();
	}

	@Override
	public void stopVentilation() throws ComponentException {
		ventilation.stop();
	}

	@Override
	public void start(double value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws ComponentException {
		// TODO Auto-generated method stub
		
	}

}
