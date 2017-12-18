package de.thebox.control.core.component;

public interface HeatPumpService extends ComponentService {
	
	public void startHeating(Long timestamp);
	
	public void stopHeating(Long timestamp);
	
	public void scheduleHeating(long start, long stop, double energy);

}
