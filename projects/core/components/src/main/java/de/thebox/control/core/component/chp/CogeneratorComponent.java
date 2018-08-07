package de.thebox.control.core.component.chp;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.CogeneratorService;
import de.thebox.control.core.component.ComponentImpl;
import de.thebox.control.core.component.circ.Circulation;
import de.thebox.control.core.component.circ.pump.CirculationPump;
import de.thebox.control.core.component.vent.CabinetVentilation;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.Value;

public abstract class CogeneratorComponent extends ComponentImpl implements CogeneratorService {

	private Circulation circulation;
	private CirculationPump circulationPump;
	private CabinetVentilation ventilation;

	protected volatile boolean maintenance = false;

	@Override
	public void activate(Preferences config) throws ControlException {
		circulation = new Circulation(control, config);
		circulationPump = new CirculationPump(control, circulation, config);
		ventilation = new CabinetVentilation(control, config);
	}

	@Override
	public void deactivate() {
		circulationPump.deactivate();
		circulation.deactivate();
		ventilation.deactivate();
	}

	@Override
	protected void maintenance(boolean enabled) throws ControlException {
		ventilation.setEnabled(!enabled);
		maintenance = enabled;
	}

	@Override
	protected ChannelValues build(Value value) throws ControlException {
		if (value.doubleValue() < 0) {
			
		}
		else if (value.doubleValue() == 0) {
			return stop();
		}
		return start(value);
	}

}
