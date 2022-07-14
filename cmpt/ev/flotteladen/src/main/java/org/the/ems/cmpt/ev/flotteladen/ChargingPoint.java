package org.the.ems.cmpt.ev.flotteladen;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ev.ElectricVehicle;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;


@Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID+".flotteladen",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ChargingPoint extends ElectricVehicle {
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

}
