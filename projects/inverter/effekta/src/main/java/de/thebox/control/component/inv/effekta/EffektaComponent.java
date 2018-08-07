package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.MaintenanceException;
import de.thebox.control.core.component.inv.InverterComponent;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.DoubleValue;
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
		objective.getChannel().write(value);
	}

	@Override
	protected void update() throws ControlException {
		Value value = objective.getLatestValue();
		try {
			ChannelValues channels = build(value);
			for (Channel channel : channels.keySet()) {
				channel.write(channels.get(channel));
			}
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		}
	}

	@Override
	public ChannelValues objective(Value value) throws ComponentException {
		// TODO: Implement different channels and choices here
		return new ChannelValues();
	}

	@Override
	protected Value process(Value value) throws ComponentException {
		double result = value.doubleValue() + consumptionLast.doubleValue();
		
		if (external.isEnabled()) {
			result -= external.getPv().doubleValue();
		}
		return new DoubleValue(result, value.getTime());
	}

	private class ObjectiveControlListener extends ChannelListener {

		public ObjectiveControlListener(Channel channel) {
			super(channel);
		}

		@Override
		public void onValueReceived(Value value) {
			try {
				update();
				
			} catch (ControlException e) {
				logger.debug("Unable to updating inverter objective: {}", e.getMessage());
			}
		}
	}

}