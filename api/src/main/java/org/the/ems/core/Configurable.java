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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationCollection;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public abstract class Configurable {

	private final Map<String, Channel> channels = new HashMap<String, Channel>();

	private Configurations configs;

	private String section = Configurations.GENERAL;

	protected Configurable(String section) {
		this.section = section;
	}

	protected Configurable() {
	}

	@SuppressWarnings("unchecked")
	public final <C extends Configurable> C configure(Configurations configs) throws ConfigurationException {
		this.doConfigure(configs);
		return (C) this;
	}

	void doConfigure(Configurations configs) throws ConfigurationException {
		this.configs = configs;
		
		List<AnnotatedElement> elements = new LinkedList<AnnotatedElement>();
		Class<?> clazz = this.getClass();
		while(clazz.getSuperclass() != null) {
			elements.addAll(Arrays.asList(clazz.getDeclaredFields()));
			elements.addAll(Arrays.asList(clazz.getDeclaredMethods()));
		    clazz = clazz.getSuperclass();
		}
		if (isEnabled()) {
			doConfigure(configs, elements);
			onConfigure(configs);
		}
	}

	void doConfigure(Configurations configs, List<AnnotatedElement> elements) 
			throws ConfigurationException {
		
		for (AnnotatedElement element : elements) {
			Configuration config = element.getAnnotation(Configuration.class);
			if (config == null) {
				continue;
			}
			
			String section = config.section();
			if (section.isEmpty() || section.equals(Configuration.SECTION_DEFAULT)) {
				section = getConfiguredSection();
			}
			if (!configs.isEnabled(section)) {
				continue;
			}
			String[] keys = config.value();
			
			boolean configured = false;
			if (element instanceof Field) {
				configured = configureField((Field) element, section, keys, config.scale());
			}
			else {
				configured = configureMethod((Method) element, section, keys);
			}
			if (!configured && config.mandatory()) {
				throw newConfigException(MessageFormat.format("Mandatory configuration of section \"{0}\" not found: {1}",
						section, parseKey(keys, element)));
			}
		}
	}

	protected void onConfigure(Configurations configs) throws ConfigurationException {
		// Default implementation to be overridden
	}

	private boolean configureMethod(Method method,
			String section, String[] keys) throws ConfigurationException {
		
		if (keys.length > 1) {
			throw newConfigException(MessageFormat.format("Method configuration \"{0}\" with several keys not allowed",
					method.getName()));
		}
		
		String key = parseKey(keys, method);
		if (configs.containsKey(section, key)) {
			configureChannel(section, key);
			
			return true;
		}
		return false;
	}

	private boolean configureField(Field field, 
			String section, String[] keys, double scale) throws ConfigurationException {
		Object value = null;
		
		Class<?> type = field.getType();
		if (ChannelCollection.class.isAssignableFrom(type)) {
			value = configureChannels(section, keys);
		}
		else if (ConfigurationCollection.class.isAssignableFrom(type)) {
			value = configureCollection(section, keys, type);
		}
		else if (Collection.class.isAssignableFrom(type)) {
			value = configureList(section, keys);
		}
		else {
			for (String key : keys) {
				if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
					key = parseKey(field.getName());
				}
				if (configs.containsKey(section, key)) {
					value = configureField(type, section, key);
					break;
				}
			}
		}
		try {
			if (value != null) {
				if (scale != 1) {
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
				field.setAccessible(true);
				field.set(this, value);
				
				if (type.isAssignableFrom(ChannelCollection.class) &&
						keys.length > ((ChannelCollection) value).size()) {
					return false;
				}
				else if (type.isAssignableFrom(Collection.class) &&
						keys.length > ((Collection<?>) value).size()) {
					return false;
				}
				return true;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw newConfigException(e.getMessage());
		}
		return false;
	}

	private Object configureField(Class<?> type, 
			String section, String key) throws ConfigurationException {

		try {
			if (Channel.class.isAssignableFrom(type)) {
				Channel channel = configureChannel(section, key);
				
				return channel;
			}
			else {
				return configs.get(section, key, type);
			}
		} catch (ConfigurationException | IllegalArgumentException | 
				NullPointerException | NoSuchFieldException e) {
			
			throw newConfigException(e.getMessage());
		}
	}

	private Collection<String> configureList(String section, String[] keys) throws ConfigurationException {
		Collection<String> collection = new LinkedList<String>();
		for (String key : keys) {
			try {
				if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
					throw new ConfigurationException("Error configuring empty string collection");
				}
				else if (key.contains("?") || key.contains("*")) {
					for (String k : configs.search(section, key)) {
						collection.add(configs.get(section, k, String.class));
					}
				}
				else if (configs.containsKey(section, key)) {
					collection.add(configs.get(section, key, String.class));
				}
			} catch (ConfigurationException | IllegalArgumentException | 
					NullPointerException | NoSuchFieldException e) {
				
				throw newConfigException(e.getMessage());
			}
		}
		return collection;
	}

	private ConfigurationCollection<?> configureCollection(String section, String[] keys, 
			Class<?> type) throws ConfigurationException {
        try {
        	ConfigurationCollection<?> collection = (ConfigurationCollection<?>) type.getDeclaredConstructor().newInstance();
    		for (String key : keys) {
    			try {
    				if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
    					throw new ConfigurationException("Error configuring empty string collection");
    				}
    				else if (key.contains("?") || key.contains("*")) {
    					for (String k : configs.search(section, key)) {
    						collection.add(k, configs.get(section, k, String.class));
    					}
    				}
    				else if (configs.containsKey(section, key)) {
    					collection.add(key, configs.get(section, key, String.class));
    				}
    			} catch (ConfigurationException | IllegalArgumentException | 
    					NullPointerException | NoSuchFieldException e) {
    				
    				throw newConfigException(e.getMessage());
    			}
    		}
    		return collection;
    		
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | 
        		SecurityException | NoSuchMethodException e) {
			throw newConfigException(e.getMessage());
        }
	}

	private ChannelCollection configureChannels(String section, String[] keys) throws ConfigurationException {
		ChannelCollection channels = new ChannelCollection();
		for (String key : keys) {
			if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
				throw new ConfigurationException("Error configuring empty channel collection");
			}
			else if (key.contains("?") || key.contains("*")) {
				for (String k : configs.search(section, key)) {
					Channel channel = configureChannel(section, k);
					channels.put(k, channel);
				}
			}
			else if (configs.containsKey(section, key)) {
				Channel channel = configureChannel(section, key);
				channels.put(key, channel);
			}
		}
		return channels;
	}

	private Channel configureChannel(String section, String key) throws ConfigurationException {

		String channelId = configs.get(section, key);
		if (channels.containsKey(channelId)) {
			return channels.get(channelId);
		}
		try {
			Channel channel = new ChannelListener(getContentManagement().getChannel(channelId));
			channels.put(channelId, channel);
			
			return channel;
			
		} catch (UnknownChannelException e) {
			throw new ConfigurationException(MessageFormat.format("Unknown channel \"{0}\" for in section {1}", 
					channelId, section));
			
		} catch (UnsupportedOperationException | NullPointerException e) {
			throw new ConfigurationException(MessageFormat.format("Unable to configure channel \"{0}\" in section {1}", 
					channelId, section));
		}
	}

	protected ContentManagementService getContentManagement() {
		// Default implementation to be overridden
		throw new UnsupportedOperationException();
	}

	protected Channel getConfiguredChannel(String key, String section) throws ConfigurationException {
		String channelId = configs.get(section, key);
		Channel channel = channels.get(channelId);
		if (channel == null) {
			channel = configureChannel(section, key);
		}
		return channel;
	}

	protected Channel getConfiguredChannel(String key) throws ConfigurationException {
		return getConfiguredChannel(key, 
				getConfiguredSection());
	}

	protected Channel getConfiguredChannel(String key, ValueListener listener) throws ConfigurationException {
		Channel channel =  getConfiguredChannel(key);
		channel.registerValueListener(listener);
		return channel;
	}

	protected void registerConfiguredValueListener(String key, ValueListener listener) throws ConfigurationException {
		Channel channel =  getConfiguredChannel(key);
		channel.registerValueListener(listener);
	}

	protected void deregisterConfiguredValueListener(String key, ValueListener listener) throws ConfigurationException {
		Channel channel =  getConfiguredChannel(key);
		channel.deregisterValueListener(listener);
	}

	protected void deregisterConfiguredValueListeners(String key) throws ConfigurationException {
		Channel channel =  getConfiguredChannel(key);
		channel.deregisterValueListeners();
	}

	protected void deregisterConfiguredValueListeners() {
		channels.values().stream().forEach(c -> c.deregisterValueListeners());
	}

	protected Value getConfiguredValue(String key, ValueListener listener) throws ComponentException, InvalidValueException {
		Channel channel =  getConfiguredChannel(key);
		channel.registerValueListener(listener);
		return channel.getLatestValue();
	}

	protected Value getConfiguredValue(String key) throws ComponentException, InvalidValueException {
		Channel channel =  getConfiguredChannel(key);
		return channel.getLatestValue();
	}

	public Configurations getConfigurations() {
		return configs;
	}

	protected String getConfiguredSection() {
		return section;
	}

	@SuppressWarnings("unchecked")
	protected <C extends Configurable> C setConfiguredSection(String section) {
		this.section = section;
		return (C) this;
	}

	public boolean isEnabled() {
		return getConfigurations().isEnabled(section);
	}

	private ConfigurationException newConfigException(String message) {
		return new ConfigurationException(MessageFormat.format("Error reading configuration \"{0}\": {1}", 
				this.getClass().getSimpleName(), message));
	}

	private static String parseKey(String[] keys, AnnotatedElement element) {
		String key = keys[0];
		if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
			String name;
			if (element instanceof Field) {
				name = ((Field) element).getName();
			}
			else {
				name = ((Method) element).getName();
				if (name.startsWith("get")) {
					name = name.substring(3);
				}
			}
			key = parseKey(name);
		}
		return key;
	}

	private static String parseKey(String key) {
		LinkedList<String> result = new LinkedList<String>();
		for (String str : key.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
			result.add(str.toLowerCase());
		}
		return String.join("_", result);
	}

}
