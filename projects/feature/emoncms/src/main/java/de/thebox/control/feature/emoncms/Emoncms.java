package de.thebox.control.feature.emoncms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.emoncms.Feed;
import org.emoncms.com.EmoncmsException;
import org.emoncms.com.EmoncmsUnavailableException;
import org.emoncms.com.http.HttpEmoncmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.data.ValueListener;

public class Emoncms extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(Emoncms.class);

	private final int interval;

	private final org.emoncms.Emoncms connection;
	private final Map<String, FeedListener> listeners = new HashMap<String, FeedListener>();

	private volatile boolean deactivateFlag;

	public Emoncms(Preferences prefs) throws ControlException {
		EmoncmsConfig config = new EmoncmsConfig(prefs);
		this.interval = config.getInterval();
		try {
			logger.debug("Activating emoncms observer at \"{}\"", config.getAddress());
			
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

	public void registerFeedListener(String name, ValueListener listener) throws ControlException {
		try {
			List<Feed> feeds = connection.getFeedList();
			
			FeedListener feedListener = null;
			for (Feed feed : feeds) {
				if (feed.getName().equals(name)) {
					feedListener = new FeedListener(feed, listener);
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

	public void deregisterFeedListener(String name) {
		listeners.remove(name);
	}

	@Override
	public void run() {
		setName("TH-E Feature emoncms observer");
		
		while (!deactivateFlag) {
			long start = System.currentTimeMillis();
			try {
				for (FeedListener feed : listeners.values()) {
					try {
						feed.poll();
						
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
