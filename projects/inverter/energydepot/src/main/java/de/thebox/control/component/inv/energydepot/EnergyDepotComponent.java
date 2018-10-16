package de.thebox.control.component.inv.energydepot;

import java.util.prefs.Preferences;

import org.osgi.service.component.annotations.Component;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.inv.InverterComponent;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

@Component
public class EnergyDepotComponent extends InverterComponent {
	private final static String ID = "EnergyDepot";

	protected Channel inverterObjective;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void activate(Preferences prefs) throws ControlException {
		super.activate(prefs);
		
		EnergyDepotConfig config = new EnergyDepotConfig(prefs);
		inverterObjective = control.getChannel(config.getInverterObjective());
	}

	@Override
	public ChannelValues objective(Value value) throws ComponentException {
		double objective = value.doubleValue() + consumption.getLatestValue().doubleValue();
		
		if (objective > objectiveMax) {
			objective = objectiveMax;
		}
		else if (objective < objectiveMin) {
			objective = objectiveMin;
		}
		return new ChannelValues(inverterObjective, new DoubleValue(objective, value.getTime()));
	}

}