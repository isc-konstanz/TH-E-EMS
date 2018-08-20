package de.thebox.control.core.component;

import java.util.LinkedHashMap;

import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueList;

public class ComponentWriteContainer extends LinkedHashMap<Channel, ValueList> {
	private static final long serialVersionUID = 7352264303947891380L;

	public ComponentWriteContainer() {
		super();
	}

	public boolean add(Channel channel, Value value) {
		if (!containsKey(channel)) {
			put(channel, new ValueList());
		}
		return get(channel).add(value);
	}

	public void add(ComponentWriteContainer channels) {
		if (channels == null) {
			return;
		}
		for (Channel channel : channels.keySet()) {
			for (Value value : channels.get(channel)) {
				add(channel, value);
			}
		}
	}

}
