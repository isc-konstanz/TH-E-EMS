package de.thebox.control.feature.emoncms;

import org.emoncms.Feed;
import org.emoncms.com.EmoncmsException;
import org.emoncms.data.Timevalue;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.ValueListener;

public class FeedListener {

	private final Feed feed;
	private final ValueListener listener;

	public FeedListener(Feed feed, ValueListener listener) {
		this.listener = listener;
		this.feed = feed;
		this.feed.clear();
	}

	public void poll() throws EmoncmsException {
		Timevalue value = feed.getLatestTimevalue();
		listener.onValueReceived(new DoubleValue(value.getValue(), value.getTime()));
	}
}
