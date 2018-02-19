package de.thebox.control.feature.emoncms;

import org.emoncms.Feed;
import org.emoncms.com.EmoncmsException;
import org.emoncms.data.Timevalue;

import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.ValueListener;

public class FeedListener {

	private final Feed feed;
	private final ValueListener listener;

	private Timevalue lastValue;

	public FeedListener(Feed feed, ValueListener listener) throws EmoncmsException {
		this.listener = listener;
		this.feed = feed;
		this.feed.clear();
		
		lastValue = this.feed.getLatestTimevalue();
	}

	public void poll() throws EmoncmsException {
		Timevalue value = feed.getLatestTimevalue();
		if (lastValue == null || (value != null && value.getTime() > lastValue.getTime())) {
			lastValue = value;
			listener.onValueReceived(new DoubleValue(value.getValue(), value.getTime()*1000));
		}
	}
}
