package de.thebox.control.component.inv.energydepot.external;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.feature.emoncms.Emoncms;


public class External {

	private volatile boolean enabled = false;
	private ChannelListener enabledListener;

	private Channel actualPower;
	private Channel virtualPower;
	private String pvFeed;
	private Value pvPower = DoubleValue.emptyValue();

	private Emoncms emoncms;

	public External(ControlService control, Preferences prefs) throws ControlException {
		ExternalConfig externalConfig = new ExternalConfig(prefs);
		try {
			emoncms = new Emoncms(prefs);
			
			enabled = true;
			enabledListener = registerEnabledListener(control.getChannel(externalConfig.getEnabled()));
			enabledListener.getChannel().setLatestValue(new BooleanValue(enabled));
			
			pvFeed = externalConfig.getPvFeed();
			registerPvListener(pvFeed);
			
			actualPower = control.getChannel(externalConfig.getActualPower());
			virtualPower = control.getChannel(externalConfig.getVirtualPower());
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid external configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerEnabledListener(Channel channel) throws ControlException {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					enabled = value.booleanValue();
				}
			}
		};
		return listener;
	}

	private ValueListener registerPvListener(String id) throws ControlException {
		ValueListener listener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					pvPower = value;
				}
			}
		};
		emoncms.registerFeedListener(id, listener);
		
		return listener;
	}

	public void deactivate() {
		if (enabledListener != null) {
			enabledListener.deregister();
		}
		if (emoncms != null) {
			emoncms.deregisterFeedListener(pvFeed);
			emoncms.deactivate();
		}
	}

	public void disable() {
		setEnabled(false);
	}

	public void enable() {
		setEnabled(true);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		enabledListener.getChannel().setLatestValue(new BooleanValue(enabled));
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Value getPv() {
		return pvPower;
	}

	public void update(long timestamp) {
		if (enabled) {
			Value actualValue = actualPower.getLatestValue();
			if (actualValue != null && actualValue.getTimestamp() == timestamp) {
				Value virtualValue = new DoubleValue(actualValue.doubleValue() - pvPower.doubleValue(), timestamp);
				virtualPower.setLatestValue(virtualValue);
			}
		}
	}
}
