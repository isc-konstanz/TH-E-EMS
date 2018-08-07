package de.thebox.control.core.component.vent;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CabinetVentilationConfig extends Configuration {

	public final static String SECTION = "Ventilation";

	public final static String VEMNTILATION_STATE_KEY = "ventState";

	public final static String TOP_TEMPERATURE_KEY = "cabinetTopTemp";
	public final static String CENTER_TEMPERATURE_KEY = "cabinetMidTemp";
	public final static String BOTTOM_TEMPERATURE_KEY = "cabinetBotTemp";

	public final static String TOLERANCE_KEY = "cabinetTempTolerance";
	public final static double TOLERANCE_DEFAULT = 0.5;

	public final static String MAX_TEMPERATURE_KEY = "cabinetTempMax";
	public final static double MAX_TEMPERATURE_DEFAULT = 50;

	public final static String INTERVAL_KEY = "intervalMin";
	public final static int INTERVAL_DEFAULT = 15;

	public CabinetVentilationConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getStateKey() throws ConfigurationException {
		return getKey(VEMNTILATION_STATE_KEY);
	}

	public String getTopTemperatureKey() throws ConfigurationException {
		return getKey(TOP_TEMPERATURE_KEY);
	}

	public String getCenterTemperatureKey() throws ConfigurationException {
		return getKey(CENTER_TEMPERATURE_KEY);
	}

	public String getBottomTemperatureKey() throws ConfigurationException {
		return getKey(BOTTOM_TEMPERATURE_KEY);
	}

	public double getTemperatureTolerance() {
		return config.getDouble(TOLERANCE_KEY, TOLERANCE_DEFAULT);
	}

	public double getTemperatureMax() {
		return config.getDouble(MAX_TEMPERATURE_KEY, MAX_TEMPERATURE_DEFAULT);
	}

	public int getIntervalMin() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
