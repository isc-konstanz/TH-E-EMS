package de.thebox.control.feature.emoncms;

import java.io.IOException;
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

import de.thebox.control.core.data.ValueListener;

public class EmoncmsObserver extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(EmoncmsObserver.class);

	private final int interval;

	private final Emoncms connection;
	private final Map<String, FeedListener> listeners = new HashMap<String, FeedListener>();

	private volatile boolean deactivateFlag;

	public EmoncmsObserver(Preferences prefs) throws EmoncmsObserverException {
		this.interval = prefs.getInt(EmoncmsConst.INTERVAL_KEY, EmoncmsConst.INTERVAL_DEFAULT);
		try {
			EmoncmsConfig configs = new EmoncmsConfig(prefs.get(EmoncmsConst.CONFIGS_KEY, EmoncmsConst.CONFIGS_DEFAULT));
			
			logger.debug("Activating emoncms observer at \"{}\"", configs.getAddress());
			
			String address = configs.getAddress();
			int maxThreads = configs.getMaxThreads();
			if (configs.hasAuthentication()) {
				String authorization = configs.getAuthorization();
				String authentication = configs.getAuthentication();
				
				connection = HttpEmoncmsFactory.newAuthenticatedConnection(address, authorization, authentication, maxThreads);
			}
			else {
				connection = HttpEmoncmsFactory.newConnection(address, maxThreads);
			}
			connection.start();
			start();
			
		} catch (IOException | EmoncmsUnavailableException e) {
			throw new EmoncmsObserverException("Error while activating emoncms observer: " + e.getMessage());
		}
	}

	protected void deactivate() {
		deactivateFlag = true;
		
		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	public void registerFeedListener(String name, ValueListener listener) throws EmoncmsObserverException {
		try {
			List<Feed> feeds = connection.getFeedList();
			
			FeedListener feedListener = null;
			for (Feed feed : feeds) {
				if (feed.getName().equals(name)) {
					feedListener = new FeedListener(feed, listener);
				}
			}
			if (feedListener == null) {
				throw new EmoncmsObserverException("Unable to register listener for feed: " + name);
			}
			listeners.put(name, feedListener);
			
		} catch (EmoncmsException e) {
			throw new EmoncmsObserverException("Error while requesting feed list: " + e.getMessage());
		}
	}

	public void deregisterFeedListener(String name) {
		listeners.remove(name);
	}

	@Override
	public void run() {
		setName("TH-E Feature emoncms observer");
		
		while (!deactivateFlag) {
			try {
				for (FeedListener feed : listeners.values()) {
					try {
						feed.poll();
						
					} catch (EmoncmsException e) {
						logger.warn("Error while reading feed value: {}", e.getMessage());
					}
				}
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		}
	}

}
