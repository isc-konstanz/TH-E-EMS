package de.thebox.control.opt.service;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.feature.emoncms.Emoncms;
import de.thebox.control.opt.Optimization;

public class GridService {

	/**
	 * Interface used to notify the {@link Optimization} 
	 * implementation about received grid service requests
	 */
	public interface GridServiceCallbacks {
		public void onGridServiceRequest(Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of received grid service requests
	 */
	private final GridServiceCallbacks callbacks;

	private final Channel gridService;

	private final Emoncms emoncms;
	private final String externalServiceFeed;

	public GridService(GridServiceCallbacks callbacks, ControlService control, Preferences prefs) throws ComponentException {
		this.callbacks = callbacks;
		
		GridServiceConfig config = new GridServiceConfig(prefs);
		try {
			gridService = control.getChannel(config.getGridService());
			
			emoncms = new Emoncms(prefs);
			
			externalServiceFeed = config.getExternalServiceFeed();
			registerExternalServiceListener(externalServiceFeed);
			
		} catch (ControlException e) {
			throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
		}
	}

	private ValueListener registerExternalServiceListener(String id) throws ControlException {
		ValueListener listener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					gridService.setLatestValue(value);
					callbacks.onGridServiceRequest(value);
				}
			}
		};
		emoncms.registerFeedListener(id, listener);
		
		return listener;
	}

	public void deactivate() {
		if (emoncms != null) {
			emoncms.deregisterFeedListener(externalServiceFeed);
			emoncms.deactivate();
		}
	}
}
