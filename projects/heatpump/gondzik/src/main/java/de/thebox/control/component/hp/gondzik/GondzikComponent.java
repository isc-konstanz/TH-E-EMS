package de.thebox.control.component.hp.gondzik;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.hp.HeatPumpComponent;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.Value;

@Component
public class GondzikComponent extends HeatPumpComponent {
	private final static String ID = "Gondzik";

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
	public ChannelValues start(Value value) throws ComponentException {
		// TODO Auto-generated method stub
		return new ChannelValues();
	}

	@Override
	public ChannelValues stop() throws ComponentException {
		// TODO Auto-generated method stub
		return new ChannelValues();
	}

}
