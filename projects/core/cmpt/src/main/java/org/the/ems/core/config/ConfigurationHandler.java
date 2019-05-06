/* 
 * Copyright 2016-19 ISC Konstanz
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
package org.the.ems.core.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

public abstract class ConfigurationHandler {

	protected static final String SECTION_DEFAULT = "General";

	private String section = SECTION_DEFAULT;

	private boolean disabled = true;

	protected final Map<String, Channel> channels = new HashMap<String, Channel>();

	protected ContentManagementService context;

	public void onBind(ContentManagementService context) throws EnergyManagementException {
		this.context = context;
	}

	public void onConfigure(Configurations configs) throws ConfigurationException {
		List<AnnotatedElement> elements = new LinkedList<AnnotatedElement>();
		elements.addAll(Arrays.asList(this.getClass().getDeclaredFields()));
		elements.addAll(Arrays.asList(this.getClass().getDeclaredMethods()));
		
		for (AnnotatedElement element : elements) {
			Configuration config = element.getAnnotation(Configuration.class);
			if (config == null) {
				continue;
			}
			
			String section = config.section();
			if (section.isEmpty() || section.equals(Configuration.SECTION_DEFAULT)) {
				section = getConfiguredSection();
			}
			if (configs.isDisabled(section)) {
				continue;
			}
			String[] keys = config.value();
			
			boolean configured = false;
			if (element instanceof Field) {
				configured = onConfigure(configs, (Field) element, keys, config.scale());
			}
			else {
				configured = onConfigure(configs, (Method) element, keys);
			}
			if (!configured && config.mandatory()) {
				throw newConfigException(MessageFormat.format("Mandatory configuration of section \"{0}\" not found: {1}",
						section, keys));
			}
		}
		if (!configs.isDisabled(section)) {
			disabled = false;
		}
	}

	private boolean onConfigure(Configurations configs, Method method, String[] keys) 
			throws ConfigurationException {
		
		if (keys.length > 1) {
			throw newConfigException(MessageFormat.format("Method configuration \"{0}\" with several keys not allowed",
					method.getName()));
		}
		
		String key = keys[0];
		if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
			key = Configurations.parse(method.getName().substring(3));
		}
		if (configs.contains(section, key)) {
			onConfigureChannel(configs, section, key);
			
			return true;
		}
		return false;
	}

	private boolean onConfigure(Configurations configs, Field field, String[] keys, double scale) 
			throws ConfigurationException {
		
		Object value = null;
		
		for (String key : keys) {
			if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
				key = Configurations.parse(field.getName());
			}
			if (configs.contains(section, key)) {
				value = onConfigureField(configs, field, section, key);
				break;
			}
		}
		try {
			if (value != null) {
				if (scale != 1) {
					Class<?> type = field.getType();
					if (type.isAssignableFrom(short.class) || type.isAssignableFrom(Short.class)) {
						value = (short) value*(short)scale;
					}
					else if (type.isAssignableFrom(int.class) || type.isAssignableFrom(Integer.class)) {
						value = (int) value*(int) scale;
					}
					else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
						value = (long) value*(long) scale;
					}
					else if (type.isAssignableFrom(float.class) || type.isAssignableFrom(Float.class)) {
						value = (float) value*(float) scale;
					}
					else if (type.isAssignableFrom(double.class) || type.isAssignableFrom(Double.class)) {
						value = (double) value*scale;
					}
				}
				field.set(this, value);
				
				return true;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw newConfigException(e.getMessage());
		}
		return false;
	}

	private Object onConfigureField(Configurations configs, Field field, 
			String section, String key) throws ConfigurationException {
		
		try {
			field.setAccessible(true);
			
			Class<?> type = field.getType();
			if (type.isAssignableFrom(Channel.class)) {
				Channel channel = onConfigureChannel(configs, section, key);
				
				if (type.isAssignableFrom(ChannelListener.class)) {
					return new ChannelListener(channel);
				}
				else {
					return channel;
				}
			}
			else {
				return configs.get(section, key, type);
			}
		} catch (ConfigurationException | IllegalArgumentException | 
				NullPointerException | NoSuchFieldException e) {
			
			throw newConfigException(e.getMessage());
		}
	}

	private Channel onConfigureChannel(Configurations configs,  
			String section, String key) throws ConfigurationException {
		
		if (channels.containsKey(key)) {
			return channels.get(key);
		}
		if (context == null) {
			throw newConfigException("Unable to configure channel");
		}
		try {
			String id = configs.getString(section, key);
			Channel channel = context.getChannel(id);
			channels.put(key, channel);
			
			return channel;
			
		} catch (UnknownChannelException | NullPointerException e) {
			throw new ConfigurationException(e);
		}
	}

	protected Channel getConfiguredChannel(String key) {
		return channels.get(key);
	}

	protected Channel getConfiguredChannel() throws ComponentException {
		String key = getConfiguredKey();
		return this.getConfiguredChannel(key);
	}

	protected Value getConfiguredValue(String key) throws ComponentException {
		return getConfiguredChannel(key).getLatestValue();
	}

	protected Value getConfiguredValue() throws ComponentException {
		return getConfiguredChannel().getLatestValue();
	}

	protected String getConfiguredKey() throws ComponentException {
		StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (int i = stackTrace.length - 1; i >= 0; i--) {
			try {
				String method = stackTrace[i].getMethodName();
				
				Configuration config = this.getClass().getMethod(method).getAnnotation(Configuration.class);
				if (config == null) {
					continue;
				}
				String key = config.value()[0];
				if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
					key = Configurations.parse(method.substring(3));
				}
				return key;
				
			} catch (NoSuchMethodException | SecurityException e) {
			}
		}
		throw new ComponentException("Error retrieving configured method key");
	}

	protected String getConfiguredSection() {
		return section;
	}

	@SuppressWarnings("unchecked")
	protected <C extends ConfigurationHandler> C setConfiguredSection(String section) {
		this.section = section;
		return (C) this;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	private ConfigurationException newConfigException(String message) {
		return new ConfigurationException(MessageFormat.format("Error reading configuration \"{0}\": {1}", 
				this.getClass().getSimpleName(), message));
	}

}
