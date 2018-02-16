package de.thebox.control.core.component;

public interface CabinetService extends ComponentService {

	public void startVentilation() throws ComponentException;

	public void stopVentilation() throws ComponentException;

}
