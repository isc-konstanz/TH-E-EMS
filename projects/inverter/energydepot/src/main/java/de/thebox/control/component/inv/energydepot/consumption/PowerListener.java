package de.thebox.control.component.inv.energydepot.consumption;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.Value;

public class PowerListener extends ChannelListener {

	/**
	 * Interface used to notify the {@link Consumption} 
	 * implementation about changed power values
	 */
	public interface PowerCallbacks {
		public void onPowerValueReceived(PowerType type, Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed power values
	 */
	private final PowerCallbacks callbacks;

	private final PowerType type;

	public PowerListener(PowerCallbacks callbacks, PowerType type, Channel channel) {
		super(channel);
		
		this.callbacks = callbacks;
		this.type = type;
	}

	public PowerType getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		if (value != null) {
			callbacks.onPowerValueReceived(type, value);
		}
	}
}
