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
package de.thebox.control.core.component.remote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.emoncms.Emoncms;
import org.emoncms.Feed;
import org.emoncms.com.EmoncmsException;
import org.emoncms.com.EmoncmsUnavailableException;
import org.emoncms.com.http.HttpEmoncmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.data.ValueListener;

public class Remote extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(Remote.class);

	private final int interval;

	private final Emoncms connection;
	private final Map<String, RemoteListener> listeners = new HashMap<String, RemoteListener>();

	private volatile boolean deactivateFlag;

	public Remote(Preferences prefs) throws ControlException {
		RemoteConfig config = new RemoteConfig(prefs);
		this.interval = config.getInterval();
		try {
			logger.debug("Activating remote emoncms observer at \"{}\"", config.getAddress());
			
			String address = config.getAddress();
			int maxThreads = config.getMaxThreads();
			if (config.hasAuthentication()) {
				String authorization = config.getAuthorization();
				String authentication = config.getAuthentication();
				
				connection = HttpEmoncmsFactory.newAuthenticatedConnection(address, authorization, authentication, maxThreads);
			}
			else {
				connection = HttpEmoncmsFactory.newConnection(address, maxThreads);
			}
			connection.start();
			start();
			
		} catch (EmoncmsUnavailableException e) {
			throw new ControlException("Error while activating emoncms observer: " + e.getMessage());
		}
	}

	public void deactivate() {
		deactivateFlag = true;
		
		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	public void registerListener(String name, ValueListener listener) throws ControlException {
		try {
			List<Feed> feeds = connection.getFeedList();
			
			RemoteListener feedListener = null;
			for (Feed feed : feeds) {
				if (feed.getName().equals(name)) {
					feedListener = new RemoteListener(feed, listener);
					break;
				}
			}
			if (feedListener == null) {
				throw new EmoncmsException("Unable to register listener for feed: " + name);
			}
			listeners.put(name, feedListener);
			
		} catch (EmoncmsException e) {
			throw new ControlException("Error while requesting feed list: " + e.getMessage());
		}
	}

	public void deregisterListener(String name) {
		listeners.remove(name);
	}

	@Override
	public void run() {
		setName("TH-E Feature emoncms observer");
		
		while (!deactivateFlag) {
			long start = System.currentTimeMillis();
			try {
				for (RemoteListener listener : listeners.values()) {
					try {
						listener.poll();
						
					} catch (EmoncmsException e) {
						logger.warn("Error while reading feed value: {}", e.getMessage());
					}
				}
				long sleep = interval - (System.currentTimeMillis() - start);
				if (sleep > 0) {
					Thread.sleep(sleep);
				}
			} catch (InterruptedException e) {
			}
		}
	}

}
