package org.the.cmpt.ctrl.mpc.service;

import org.the.cmpt.ctrl.mpc.Control;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

public class GridService {

	/**
	 * Interface used to notify the {@link Control} 
	 * implementation about received grid service requests
	 */
	public interface GridServiceCallbacks {
		public void onGridServiceRequest(Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of received grid service requests
	 */
	private final GridServiceCallbacks callbacks;

	private ChannelListener service;

	public GridService(GridServiceCallbacks callbacks, ContentManagementService manager, Configurations configs) throws ConfigurationException {
		this.callbacks = callbacks;
		try {
			if (configs.hasSection(GridServiceConfig.class)) {
				GridServiceConfig config = configs.getSection(GridServiceConfig.class);
				
				if (config.hasService()) {
					service = registerServiceListener(manager.getChannel(config.getService()));
				}
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid grid service configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerServiceListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				callbacks.onGridServiceRequest(value);
			}
		};
		return listener;
	}

	public void deactivate() {
		if (service != null) {
			service.deregister();
		}
	}
}
