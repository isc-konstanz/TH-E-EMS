package de.thebox.control.core.component;

public interface HeatPumpService extends ComponentService {

	@Override
	public default Component getType() {
		return Component.HEATPUMP;
	};

	public void start(double value) throws ComponentException;

	public void stop() throws ComponentException;

}
