package de.thebox.control.core.component;

import de.thebox.control.core.data.Value;

public interface CogeneratorService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.COGENERATOR;
	};

	public void start(ComponentWriteContainer container, Value value) throws ComponentException;

	public void stop(ComponentWriteContainer container) throws ComponentException;

}
