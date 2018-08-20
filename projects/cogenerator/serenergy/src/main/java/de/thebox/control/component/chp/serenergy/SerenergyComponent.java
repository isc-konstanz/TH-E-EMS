package de.thebox.control.component.chp.serenergy;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentWriteContainer;
import de.thebox.control.core.component.chp.CogeneratorComponent;
import de.thebox.control.core.data.Value;

@Component
public class SerenergyComponent extends CogeneratorComponent {
	private final static String ID = "Serenergy";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences config) throws ControlException {
		super.activate(config);
		
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
	}

	@Override
	public void start(ComponentWriteContainer container, Value value) throws ComponentException {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop(ComponentWriteContainer container) throws ComponentException {
		// TODO Auto-generated method stub
	}

}
