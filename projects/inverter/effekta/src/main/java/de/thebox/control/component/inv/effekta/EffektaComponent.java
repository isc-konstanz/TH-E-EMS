package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.inv.InverterComponent;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.Value;

@Component
public class EffektaComponent extends InverterComponent {
	private final static Logger logger = LoggerFactory.getLogger(EffektaComponent.class);

	private final static String ID = "Effekta";

	protected ChannelListener objective;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences prefs) throws ControlException {
		super.activate(prefs);
		
		EffektaConfig config = new EffektaConfig(prefs);
		objective = new ObjectiveControlListener(control.getChannel(config.getObjective()));
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		objective.deregister();
	}

	@Override
	public void set(Value value) throws ControlException {
		objective.getChannel().setLatestValue(value);
	}

	@Override
	public ChannelValues objective(Value value) throws ComponentException {
		// TODO: Implement different channels and choices here
		return new ChannelValues();
	}

	private class ObjectiveControlListener extends ChannelListener {

		public ObjectiveControlListener(Channel channel) {
			super(channel);
		}

		@Override
		public void onValueReceived(Value value) {
			update();
		}
	}

}