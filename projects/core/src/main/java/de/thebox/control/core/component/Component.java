package de.thebox.control.core.component;

public enum Component {
	NONE(null),
    INVERTER("inv"),
    COGENERATOR("chp"),
    HEATPUMP("hp");

	private final String key;

	private Component(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
