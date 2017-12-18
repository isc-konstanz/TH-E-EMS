package de.thebox.control.core.component;

public interface CogeneratorService extends ComponentService {
	
	public void startGeneration(Long timestamp);
	
	public void stopGeneration(Long timestamp);
	
	public void scheduleGeneration(long start, long stop, double energy);

}
