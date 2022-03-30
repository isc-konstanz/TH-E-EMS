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
package org.openmuc.framework.app.the.ems;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.framework.app.the.ems.ChannelWrapper.ChannelCallbacks;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.schedule.NamedThreadFactory;

@Component(
	immediate = true,
	service = ContentManagementService.class
)
public class ContentManager implements ContentManagementService, ChannelCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(ContentManager.class);

	private final Map<String, ChannelWrapper> channels = new HashMap<String, ChannelWrapper>();

	private ExecutorService executor = null;

	@Reference
	private DataAccessService access;

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E Content Management System");
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E CMS Pool - thread-");
		executor = Executors.newCachedThreadPool(namedThreadFactory);
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E Content Management System");
		
		executor.shutdown();
	}

	@Override
	public Channel getChannel(String id) throws UnknownChannelException {
		if (id == null) {
			throw new NullPointerException();
		}
		if (!channels.containsKey(id)) {
			if (!access.getAllIds().contains(id)) {
				throw new UnknownChannelException("Unknown channel for id: " + id);
			}
			ChannelWrapper channel = new ChannelWrapper(this, access.getChannel(id));
			channels.put(id, channel);
			
			return channel;
		}
		return channels.get(id);
	}

	@Override
	public void registerValueListener(String id, ValueListener listener) throws UnknownChannelException {
		getChannel(id).registerValueListener(listener);
	}

	@Override
	public void deregisterValueListener(String id, ValueListener listener) throws UnknownChannelException {
		getChannel(id).deregisterValueListener(listener);
	}

	@Override
	public Value getLatestValue(String id, ValueListener listener) throws UnknownChannelException, InvalidValueException {
		return getChannel(id).getLatestValue(listener);
	}

	@Override
	public Value getLatestValue(String id) throws UnknownChannelException, InvalidValueException {
		return getChannel(id).getLatestValue();
	}

	@Override
	public void setLatestValue(String id, Value value) throws UnknownChannelException {
		logger.debug("Set latest value for channel \"{}\": {}", id, value);
		getChannel(id).setLatestValue(value);
	}

	@Override
	public void write(String id, Value value) throws UnknownChannelException {
		logger.debug("Writing value for channel \"{}\": {}", id, value);
		getChannel(id).write(value);
	}

	@Override
	public void write(String id, ValueList values) throws UnknownChannelException {
		logger.debug("Writing values for channel \"{}\": {}", id, values);
		getChannel(id).write(values);
	}

	@Override
	public void execute(Runnable task) {
		executor.execute(task);
	}

}
