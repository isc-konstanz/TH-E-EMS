/* 
 * Copyright 2016-18 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thebox.control.core.component.inv.external;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.remote.Remote;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;


public class External {

	private volatile boolean enabled = false;
	private ChannelListener enabledListener;

	private Channel virtualPower;
	private ChannelListener activePowerListener;

	private Value pvPower = DoubleValue.emptyValue();

	private Remote remote;

	public External(ControlService control, Preferences prefs) throws ControlException {
		try {
			if (prefs.nodeExists(ExternalConfig.SECTION)) {
				ExternalConfig config = new ExternalConfig(prefs);
				
				remote = new Remote(prefs);
				
				enabled = true;
				enabledListener = registerEnabledListener(control.getChannel(config.getEnabled()));
				enabledListener.getChannel().setLatestValue(new BooleanValue(enabled));
				
				virtualPower = control.getChannel(config.getVirtualPower());
				activePowerListener = registerPowerListener(control.getChannel(config.getActualPower()));
				registerPvListener(config.getPvPower());
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid external configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerEnabledListener(Channel channel) throws ControlException {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				enabled = value.booleanValue();
			}
		};
		return listener;
	}

	private ChannelListener registerPowerListener(Channel channel) throws ControlException {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (enabled && value != null) {
					DoubleValue virtualValue = new DoubleValue(value.doubleValue() - pvPower.doubleValue(), value.getTime());
					virtualPower.setLatestValue(virtualValue);
				}
			}
		};
		return listener;
	}

	private ValueListener registerPvListener(String id) throws ControlException {
		ValueListener listener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				pvPower = value;
			}
		};
		remote.registerListener(id, listener);
		
		return listener;
	}

	public void deactivate() {
		enabledListener.deregister();
		activePowerListener.deregister();
		remote.deactivate();
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
}
