package de.thebox.control.core.component;

public interface CogeneratorService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.COGENERATOR;
	};

	public void start(double value) throws ComponentException;

	public void stop() throws ComponentException;

}
