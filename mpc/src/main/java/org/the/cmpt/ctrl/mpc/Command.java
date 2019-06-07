package org.the.cmpt.ctrl.mpc;

import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class Command extends ConfigurationHandler implements ValueListener {

	/**
	 * Interface used to notify the {@link Control} 
	 * implementation about received grid service requests
	 */
	public interface CommandCallbacks {
		public void onCommandReceived(Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of received grid service requests
	 */
	private volatile CommandCallbacks callbacks = null;

	@Configuration(mandatory=false)
	private ChannelListener command = null;

	public Command(Configurations configs) throws ConfigurationException {
		onConfigure(configs);
		if (command != null) {
			command.registerValueListener(this);
		}
		else {
			setDisabled(true);
		}
	}

	public Command register(CommandCallbacks callbacks) {
		this.callbacks = callbacks;
		return this;
	}

	public void deregister() {
		this.callbacks = null;
	}

	public void deactivate() {
		if (!isDisabled()) {
			command.deregister();
		}
	}

	@Override
	public void onValueReceived(Value value) {
		if (callbacks != null) {
			callbacks.onCommandReceived(value);
		}
	}

}
