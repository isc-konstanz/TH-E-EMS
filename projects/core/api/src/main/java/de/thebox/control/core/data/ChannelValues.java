package de.thebox.control.core.data;

import java.util.HashMap;

public class ChannelValues extends HashMap<Channel, ValueList> {
	private static final long serialVersionUID = 7352264303947891380L;

	public ChannelValues() {
		super();
	}

	public ChannelValues(Channel channel, Value value) {
		super();
		add(channel, value);
	}

	public boolean add(Channel channel, Value value) {
		if (!containsKey(channel)) {
			put(channel, new ValueList());
		}
		return get(channel).add(value);
	}

	public void add(ChannelValues channels) {
		for (Channel channel : channels.keySet()) {
			for (Value value : channels.get(channel)) {
				add(channel, value);
			}
		}
	}

}
