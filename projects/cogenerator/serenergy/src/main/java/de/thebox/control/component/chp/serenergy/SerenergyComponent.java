package de.thebox.control.component.chp.serenergy;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.CogeneratorService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ScheduleComponent;
import de.thebox.control.feature.circulation.pump.CirculationPump;

@Component
public class SerenergyComponent extends ScheduleComponent implements CogeneratorService {
	private final static String ID = "Serenergy";

	private CirculationPump circulation;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences config) throws ControlException {
		circulation = new CirculationPump(control, config);
	}

	@Override
	public void deactivate() {
		if (circulation != null) {
			circulation.deactivate();
		}
	}

	@Override
	protected void maintenance(boolean enabled) throws ControlException {
		circulation.setEnabled(!enabled);
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
