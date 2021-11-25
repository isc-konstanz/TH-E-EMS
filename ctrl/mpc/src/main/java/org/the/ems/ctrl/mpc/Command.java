package org.the.ems.ctrl.mpc;

import org.the.ems.core.Configurable;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class Command extends Configurable implements ValueListener {

	/**
	 * Interface used to notify the {@link ModelPredictiveControl} 
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
	private Channel command = null;

	public Command(Configurations configs) throws ConfigurationException {
		configure(configs);
		if (command != null) {
			command.registerValueListener(this);
		}
		else {
			setEnabled(false);
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
		if (isEnabled()) {
			command.deregisterValueListeners();
		}
	}

	@Override
	public void onValueReceived(Value value) {
		if (callbacks != null) {
			callbacks.onCommandReceived(value);
		}
	}

}
