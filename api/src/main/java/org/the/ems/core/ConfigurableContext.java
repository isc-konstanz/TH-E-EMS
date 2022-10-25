/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
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
package org.the.ems.core;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.ValueListener;

public class ConfigurableContext {

	protected final Map<String, Channel> channels = new HashMap<String, Channel>();

	protected final Configurable configurable;

	private ContentManagementService content;

	public ConfigurableContext(Configurable configurable ,ContentManagementService content) {
		this.configurable = configurable;
		this.content = content;
	}

	ContentManagementService getContentManagement() throws UnsupportedOperationException {
		if (content == null) throw new UnsupportedOperationException();
		return content;
	}

	void setContentManagement(ContentManagementService content) {
		this.content = content;
	}

	public Configurations getConfigurations() {
		return configurable.getConfigurations();
	}

	protected String getConfiguredSection() {
		return configurable.getConfiguredSection();
	}

	protected Channel newChannel(String key, String section) throws ConfigurationException {
		String channelId = getConfigurations().get(section, key);
		if (channels.containsKey(channelId)) {
			return channels.get(channelId);
		}
		try {
			Channel channel = new ChannelListener(getContentManagement().getChannel(channelId));
			channels.put(channelId, channel);
			
			return channel;
			
		} catch (UnknownChannelException e) {
			throw new ConfigurationException(MessageFormat.format("Unknown channel \"{0}\" for in section: {1}", 
					channelId, section));
			
		} catch (UnsupportedOperationException | NullPointerException e) {
			throw new ConfigurationException(MessageFormat.format("Unable to configure channel \"{0}\" in section: {1}", 
					channelId, section));
		}
	}

	public Channel getChannel(String... keys) throws ConfigurationException {
		String section = getConfiguredSection();
		for (String key : keys) {
			if (!getConfigurations().containsKey(section, key)) {
				continue;
			}
			String channelId = getConfigurations().get(section, key);
			Channel channel = channels.get(channelId);
			if (channel == null) {
				channel = newChannel(key, section);
			}
			return channel;
		}
		throw new ConfigurationException(MessageFormat.format("Unknown configurations {0} in section: {1}", 
				keys, section));
	}

	public Channel getChannel(String key, ValueListener listener) throws ConfigurationException {
		Channel channel =  getChannel(key);
		channel.registerValueListener(listener);
		return channel;
	}

	public Collection<Channel> getChannels() {
		return channels.values();
	}

	protected void deregisterValueListeners() {
		getChannels().stream().forEach(c -> c.deregisterValueListeners());
	}

}
