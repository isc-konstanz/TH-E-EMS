/* 
 * Copyright 2016-2021 ISC Konstanz
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.InvalidValueException;

public abstract class Configurable {

	private boolean enabled = false;

	private String section = Configurations.GENERAL;

	protected final ChannelCollection channels = new ChannelCollection();

	protected ContentManagementService content;

	@SuppressWarnings("unchecked")
	protected <C extends Configurable> C activate(ContentManagementService content) 
			throws ComponentException {
		this.content = content;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	protected <C extends Configurable> C activate(ContentManagementService content, Configurations configs) 
			throws ComponentException {
		this.doConfigure(content, configs);
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	protected <C extends Configurable> C configure(Configurations configs) throws ConfigurationException {
		this.doConfigure(configs);
		this.onConfigure(configs);
		return (C) this;
	}

	protected final void doConfigure(ContentManagementService content, Configurations configs) 
			throws ConfigurationException {
		this.content = content;
		this.doConfigure(configs);
	}

	protected final void doConfigure(Configurations configs) throws ConfigurationException {
		List<AnnotatedElement> elements = new LinkedList<AnnotatedElement>();
		Class<?> type = this.getClass();
		while(type.getSuperclass() != null) {
			elements.addAll(Arrays.asList(type.getDeclaredFields()));
			elements.addAll(Arrays.asList(type.getDeclaredMethods()));
		    type = type.getSuperclass();
		}
		configureElements(configs, elements);
		
		this.onConfigure(configs);
	}

	protected void onConfigure(Configurations configs) throws ConfigurationException {
		// Default implementation to be overridden
	}

	protected final void configureElements(Configurations configs, List<AnnotatedElement> elements) 
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
				configured = configureField(configs, (Field) element, section, keys, config.scale());
			}
			else {
				configured = configureMethod(configs, (Method) element, section, keys);
			}
			if (!configured && config.mandatory()) {
				throw newConfigException(MessageFormat.format("Mandatory configuration of section \"{0}\" not found: {1}",
						section, parse(keys, element)));
			}
		}
		if (configs.isEnabled(section)) {
			enabled = true;
		}
	}

	private boolean configureMethod(Configurations configs, Method method,
			String section, String[] keys) throws ConfigurationException {
		
		if (keys.length > 1) {
			throw newConfigException(MessageFormat.format("Method configuration \"{0}\" with several keys not allowed",
					method.getName()));
		}
		
		String key = parse(keys, method);
		if (configs.contains(section, key)) {
			configureChannel(configs, section, key);
			
			return true;
		}
		return false;
	}

	private boolean configureField(Configurations configs, Field field, 
			String section, String[] keys, double scale) throws ConfigurationException {
		Object value = null;
		
		Class<?> type = field.getType();
		if (ChannelCollection.class.isAssignableFrom(type)) {
			value = configureChannels(configs, section, keys);
		}
		else if (ConfigurationCollection.class.isAssignableFrom(type)) {
			value = configureCollection(configs, section, keys, type);
		}
		else if (Collection.class.isAssignableFrom(type)) {
			value = configureList(configs, section, keys);
		}
		else {
			for (String key : keys) {
				if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
					key = parse(field.getName());
				}
				if (configs.contains(section, key)) {
					value = configureField(configs, type, section, key);
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

	private Object configureField(Configurations configs, Class<?> type, 
			String section, String key) throws ConfigurationException {
		
		try {
			if (Channel.class.isAssignableFrom(type)) {
				Channel channel = configureChannel(configs, section, key);
				
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

	private Collection<String> configureList(Configurations configs, 
			String section, String[] keys) throws ConfigurationException {
		
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
				else if (configs.contains(section, key)) {
					collection.add(configs.get(section, key, String.class));
				}
			} catch (ConfigurationException | IllegalArgumentException | 
					NullPointerException | NoSuchFieldException e) {
				
				throw newConfigException(e.getMessage());
			}
		}
		return collection;
	}

	private ConfigurationCollection<?> configureCollection(Configurations configs, 
			String section, String[] keys, Class<?> type) throws ConfigurationException {
		
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
    				else if (configs.contains(section, key)) {
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

	private ChannelCollection configureChannels(Configurations configs, 
			String section, String[] keys) throws ConfigurationException {
		
		ChannelCollection channels = new ChannelCollection();
		for (String key : keys) {
			if (key.isEmpty() || key.equals(Configuration.VALUE_DEFAULT)) {
				throw new ConfigurationException("Error configuring empty channel collection");
			}
			else if (key.contains("?") || key.contains("*")) {
				for (String k : configs.search(section, key)) {
					Channel channel = configureChannel(configs, section, k);
					channels.put(k, channel);
				}
			}
			else if (configs.contains(section, key)) {
				Channel channel = configureChannel(configs, section, key);
				channels.put(key, channel);
			}
		}
		return channels;
	}

	private Channel configureChannel(Configurations configs,  
			String section, String key) throws ConfigurationException {
		
		if (channels.containsKey(key)) {
			return channels.get(key);
		}
		if (content == null) {
			throw newConfigException("Unable to configure channel");
		}
		try {
			String id = configs.get(section, key);
			Channel channel = content.getChannel(id);
			channels.put(key, channel);
			
			return channel;
			
		} catch (UnknownChannelException | NullPointerException e) {
			throw new ConfigurationException(e);
		}
	}

	protected Channel getConfiguredChannel(String key) throws ConfigurationException {
		Channel channel = channels.get(key);
		if (channel == null) {
			throw new ConfigurationException("Unable to get unconfigured channel: "+key);
		}
		return channel;
	}

	protected Channel getConfiguredChannel() throws ComponentException {
		String key = getConfiguredKey();
		return this.getConfiguredChannel(key);
	}

	protected Value getConfiguredValue(String key) throws ComponentException, InvalidValueException {
		return getConfiguredChannel(key).getLatestValue();
	}

	protected Value getConfiguredValue() throws ComponentException, InvalidValueException {
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
					if (method.startsWith("get")) {
						method = method.substring(3);
					}
					key = parse(method);
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
	protected <C extends Configurable> C setConfiguredSection(String section) {
		this.section = section;
		return (C) this;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private ConfigurationException newConfigException(String message) {
		return new ConfigurationException(MessageFormat.format("Error reading configuration \"{0}\": {1}", 
				this.getClass().getSimpleName(), message));
	}

	private static String parse(String[] keys, AnnotatedElement element) {
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
			key = parse(name);
		}
		return key;
	}

	private static String parse(String key) {
		LinkedList<String> result = new LinkedList<String>();
		for (String str : key.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
			result.add(str.toLowerCase());
		}
		return String.join("_", result);
	}

}
