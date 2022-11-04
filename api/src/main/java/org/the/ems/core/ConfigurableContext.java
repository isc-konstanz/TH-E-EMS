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
import java.util.stream.Collectors;

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

	public Collection<Channel> getConfiguredChannels(String section) {
		Collection<Object> sectionValues = getConfigurations().getSection(section).values();
		return getConfiguredChannels().stream()
				.filter(c -> sectionValues.contains(c.getId()))
				.collect(Collectors.toList());
	}

	protected Collection<Channel> getConfiguredChannels() {
		return channels.values();
	}

	private Channel getConfiguredChannel(String section, String key) throws ConfigurationException {
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

	public Channel getConfiguredChannel(String section, String... keys) throws ConfigurationException {
		for (String key : keys) {
			if (!getConfigurations().containsKey(section, key)) {
				continue;
			}
			return getConfiguredChannel(section, key);
		}
		throw new ConfigurationException(MessageFormat.format("Unknown channel configurations {0} for section: {1}", 
				String.join(", ", keys), section));
	}

	public Channel getDefaultChannel(String key, ValueListener listener) throws ConfigurationException {
		Channel channel =  getDefaultChannel(key);
		channel.registerValueListener(listener);
		return channel;
	}

	public Channel getDefaultChannel(String... keys) throws ConfigurationException {
		return getConfiguredChannel(getDefaultSection(), keys);
	}

	public Collection<Channel> getDefaultChannels() {
		return getConfiguredChannels(getDefaultSection());
	}

	public String getDefaultSection() {
		return configurable.getDefaultSection();
	}

	protected void deregisterValueListeners() {
		getConfiguredChannels().stream().forEach(c -> c.deregisterValueListeners());
	}

}
