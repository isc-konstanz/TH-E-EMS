package de.thebox.control.opt.service;

public class GridService {
//
//	/**
//	 * Interface used to notify the {@link Optimization} 
//	 * implementation about received grid service requests
//	 */
//	public interface GridServiceCallbacks {
//		public void onGridServiceRequest(Value power);
//	}
//
//	/**
//	 * The Listeners' current callback object, which is notified of received grid service requests
//	 */
//	private final GridServiceCallbacks callbacks;
//
//	private final Channel gridService;
//
//	private final Remote remote;
//	private final String externalService;
//
//	public GridService(GridServiceCallbacks callbacks, ControlService control, Preferences prefs) throws ComponentException {
//		this.callbacks = callbacks;
//		
//		GridServiceConfig config = new GridServiceConfig(prefs);
//		try {
//			gridService = control.getChannel(config.getGridService());
//			
//			remote = new Remote(prefs);
//			
//			externalService = config.getExternalServiceFeed();
//			registerExternalServiceListener(externalService);
//			
//		} catch (ControlException e) {
//			throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
//		}
//	}
//
//	private ValueListener registerExternalServiceListener(String id) throws ControlException {
//		ValueListener listener = new ValueListener() {
//			
//			@Override
//			public void onValueReceived(Value value) {
//				gridService.setLatestValue(value);
//				callbacks.onGridServiceRequest(value);
//			}
//		};
//		remote.registerListener(id, listener);
//		
//		return listener;
//	}
//
//	public void deactivate() {
//		if (remote != null) {
//			remote.deregisterListener(externalService);
//			remote.deactivate();
//		}
//	}
}
