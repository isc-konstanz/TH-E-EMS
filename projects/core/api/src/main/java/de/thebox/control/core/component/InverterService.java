package de.thebox.control.core.component;

import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.Value;

public interface InverterService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.INVERTER;
	};

	public ChannelValues objective(Value value) throws ComponentException;

}
