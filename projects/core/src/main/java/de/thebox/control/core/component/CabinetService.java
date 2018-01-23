package de.thebox.control.core.component;

public interface CabinetService extends ComponentService {

	public void startVentilation(Long timestamp) throws ComponentException;

	public void stopVentilation(Long timestamp) throws ComponentException;

}
