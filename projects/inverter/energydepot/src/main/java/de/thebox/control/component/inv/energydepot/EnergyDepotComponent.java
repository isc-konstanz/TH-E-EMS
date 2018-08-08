package de.thebox.control.component.inv.energydepot;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.inv.InverterComponent;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

@Component
public class EnergyDepotComponent extends InverterComponent {
	private final static String ID = "EnergyDepot";

	protected Channel objective;
	protected ChannelListener objectiveControl;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences prefs) throws ControlException {
		super.activate(prefs);
		
		EnergyDepotConfig config = new EnergyDepotConfig(prefs);
		objective = control.getChannel(config.getObjective());
		objectiveControl = new ObjectiveControlListener(control.getChannel(config.getObjectiveControl()));
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		objectiveControl.deregister();
	}

	@Override
	public void set(Value value) throws ControlException {
		objectiveControl.getChannel().setLatestValue(value);
	}

	@Override
	public ChannelValues objective(Value value) throws ComponentException {
		return new ChannelValues(objective, value);
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
			update();
		}
	}

}