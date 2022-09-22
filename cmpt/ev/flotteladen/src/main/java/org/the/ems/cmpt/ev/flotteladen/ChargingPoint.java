package org.the.ems.cmpt.ev.flotteladen;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ev.ElectricVehicle;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ByteValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;


@Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID+".flotteladen",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ChargingPoint extends ElectricVehicle implements ValueListener {
	private static final Logger logger = LoggerFactory.getLogger(ChargingPoint.class);

	private static final int DEFAULT_VOLTAGE = 230;
	private static final int DEFAULT_CURRENT_MIN = 6;
	private static final int DEFAULT_CURRENT_MAX = 32;

	@Configuration(mandatory = false)
	private Channel statusCode;

	@Configuration
	private Channel status;

	@Configuration(mandatory = false)
	private Channel power;

	@Configuration(mandatory = false)
	private Channel voltage;

	@Configuration
	private Channel current;

	@Configuration
	private Channel currentMin;

	@Configuration
	private Channel currentMax;

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMaxPower() {
        if (Double.isNaN(powerMax)) {
            return powerMax;
        }
        return getMinPower();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMinPower() {
        return powerMin;
    }

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		// Do not call super activation to keep run state DEFAULT
		status.registerValueListener(this);
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting ... with power setpoint of {} A", 0);
		}
		// TODO
	}

	@Override
	protected void onSet(WriteContainer container, Value setpointPower) throws ComponentException {
		// TODO
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		logger.debug("Stopping ...");
		// TODO
	}

	@Override
	public void onValueChanged(Value statusValue) {
		ChargingPointStatus status = ChargingPointStatus.valueOf(statusValue.stringValue());
		if (statusCode != null) {
			statusCode.setLatestValue(new ByteValue(status.getCode(), statusValue.getEpochMillis()));
		}
	}

}
