package de.thebox.control.core.component;

public interface CabinetService extends ComponentService {

	public void startVentilation(Long timestamp);

	public void stopVentilation(Long timestamp);

}
