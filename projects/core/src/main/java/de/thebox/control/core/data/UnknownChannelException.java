package de.thebox.control.core.data;

import de.thebox.control.core.ControlException;

public class UnknownChannelException extends ControlException {
	private static final long serialVersionUID = 7463720464639954432L;

	public UnknownChannelException() {
		super();
	}

	public UnknownChannelException(String s) {
		super(s);
	}

}
