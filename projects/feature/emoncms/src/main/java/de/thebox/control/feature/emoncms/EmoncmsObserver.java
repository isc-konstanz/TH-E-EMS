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
import org.emoncms.data.Timevalue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.ValueListener;

public class EmoncmsObserver extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(EmoncmsObserver.class);

	private final int interval;

	private final Emoncms connection;
	private final Map<ValueListener, Feed> listeners = new HashMap<ValueListener, Feed>();

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
			
			Feed feed = null;
			for (Feed f : feeds) {
				if (f.getName().equals(name)) {
					feed = f;
				}
			}
			if (feed == null) {
				throw new EmoncmsObserverException("Unable to register listener for feed: " + name);
			}
			listeners.put(listener, feed);
			
		} catch (EmoncmsException e) {
			throw new EmoncmsObserverException("Error while requesting feed list: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		setName("TH-E Feature emoncms observer");
		
		while (!deactivateFlag) {
			try {
				for (Map.Entry<ValueListener, Feed> entry : listeners.entrySet()) {
					Feed feed = entry.getValue();
					try {
						Timevalue value = feed.getLatestTimevalue();
						entry.getKey().onValueReceived(new DoubleValue(value.getValue(), value.getTime()));
						
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
