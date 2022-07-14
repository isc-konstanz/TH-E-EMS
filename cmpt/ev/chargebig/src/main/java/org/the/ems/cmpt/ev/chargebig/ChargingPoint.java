package org.the.ems.cmpt.ev.chargebig;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.UnknownChannelException;

public class ChargingPoint {
	private static final Logger logger = LoggerFactory.getLogger(ChargingPoint.class);

	private static final String SECTION = "ChargePoints";

	private final double currentLimitMin;
	private final double currentLimitMax;

	private final Channel currentLimit;
	private final Channel state;

	public ChargingPoint(double currentLimitMin, double currentLimitMax, Channel currentLimit, Channel state) {
		this.currentLimitMin = currentLimitMin;
		this.currentLimitMax = currentLimitMax;
		this.currentLimit = currentLimit;
		this.state = state;
	}

	public double getFlexibilityPower() {
		ChargingPointState state;
		try {
			state = getState();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving charge point state: {}", e.getMessage());
			state = ChargingPointState.ERROR;
		}
		switch (state) {
		case CHARGING:
			try {
				double currentLimitation = getCurrentLimit() - getCurrentLimitMinimum();
				if (currentLimitation <= 0) {
					return 0;
				}
				double powerLimitation = currentLimitation*ChargeBig.PHASE_VOLTAGE;
				return powerLimitation;
				
			} catch (InvalidValueException e) {
				logger.debug("Error retrieving charge point limit: {}", e.getMessage());
			}
			break;
		default:
			break;
		}
		return 0;
	}

	public double getCurrentLimitMinimum() {
		return currentLimitMin;
	}

	public double getCurrentLimitMaximum() {
		return currentLimitMax;
	}

	public double getCurrentLimit() throws InvalidValueException {
		return currentLimit.getLatestValue().doubleValue();
	}

	public ChargingPointState getState() throws InvalidValueException {
		return ChargingPointState.valueOf(state.getLatestValue().intValue());
	}

	public boolean isConnected() {
		try {
			return this.getState() == ChargingPointState.CHARGING ||
					this.getState() == ChargingPointState.CONNECTED;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving charge point state: {}", e.getMessage());
			return false;
		}
	}

	public boolean isCharging() {
		try {
			return this.getState() == ChargingPointState.CHARGING;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving charge point state: {}", e.getMessage());
			return false;
		}
	}

	public static List<ChargingPoint> newCollection(ContentManagementService content, Configurations configs) 
			throws ConfigurationException {
		
		Integer count = configs.getInteger(SECTION, "count");
		if (count == null || count < 1) {
			throw new ConfigurationException("Mandatory ChargePoint count not found");
		}
		Double limitMin = configs.getDouble(SECTION, "current_limit_min");
		if (limitMin == null || limitMin.isNaN()) {
			limitMin = 8.0;
		}
		Double limitMax = configs.getDouble(SECTION, "current_limit_max");
		if (limitMax == null || limitMax.isNaN()) {
			limitMax = 20.0;
		}
		
		List<ChargingPoint> chargePoints = new ArrayList<ChargingPoint>(count);
		for (int i=1; i<=count; i++) {
			ChargingPoint chargePoint = null;
			try {
				Channel limit = getConfiguredChannel(content, configs, i, "current_limit");
				Channel state = getConfiguredChannel(content, configs, i, "state");
				
				chargePoint = new ChargingPoint(limitMin, limitMax, limit, state);
				logger.debug("Registered charge point #{}", i);
				
			} catch (UnknownChannelException e) {
				logger.warn("Unable to configure charge point #{}", i);
			}
			chargePoints.add(chargePoint);
		}
		return Collections.unmodifiableList(chargePoints);
	}

	private static Channel getConfiguredChannel(ContentManagementService content, Configurations configs, 
			int number, String key) throws ConfigurationException, UnknownChannelException {
		
		String format = configs.get(SECTION, key+"_format");
		if (format == null || format.isEmpty()) {
			throw new ConfigurationException(MessageFormat.format("Mandatory ChargePoint format not found: {0}", key));
		}
		else if (!format.contains("#")) {
			throw new ConfigurationException(MessageFormat.format("Invalid ChargePoint format {0}: {1}", key, format));
		}
		String id = format.replace("#", String.valueOf(number));
		try {
			return content.getChannel(id);
			
		} catch (NullPointerException e) {
			throw new ConfigurationException(e);
		}
	}

}
