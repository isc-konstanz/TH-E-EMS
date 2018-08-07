package de.thebox.control.core.component;

import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.Value;

public interface HeatPumpService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.HEATPUMP;
	};

	public ChannelValues start(Value value) throws ComponentException;

	public ChannelValues stop() throws ComponentException;

}
