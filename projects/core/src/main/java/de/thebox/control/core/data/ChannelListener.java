package de.thebox.control.core.data;

public abstract class ChannelListener implements ValueListener {

	private final Channel channel;

	public ChannelListener(Channel channel) {
		this.channel = channel;
		this.channel.registerValueListener(this);
	}

	public Channel getChannel() {
		return channel;
	}

	public Value getLatestValue() {
		return channel.getLatestValue();
	}

	public void deregister() {
		channel.deregisterValueListener(this);
	}
}
