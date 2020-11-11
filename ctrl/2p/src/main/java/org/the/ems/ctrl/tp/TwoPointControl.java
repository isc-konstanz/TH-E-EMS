package org.the.ems.ctrl.tp;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationCollection.DoubleCollection;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.ctrl.Control;
import org.the.ems.ctrl.ControlledHeating;

@Component(
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".2p",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TwoPointControl extends Control {
	private final static Logger logger = LoggerFactory.getLogger(TwoPointControl.class);

	@Configuration("*_temp_max")
	private DoubleCollection temperaturesMax;

	@Configuration("*_temp_min")
	private DoubleCollection temperaturesMin;

	@Configuration("*_temp")
	private ChannelCollection temperatures;

	@Override
	public ControlledHeating onCreate(HeatingService heating) throws ComponentException {
		String heatingId = heating.getId();
		
		Channel temperature = temperatures.get(heatingId+"_temp");
		if (temperature != null) {
			if (!temperaturesMax.contains(heatingId+"_temp_max") ||
					!temperaturesMin.contains(heatingId+"_temp_min")) {
				throw new ConfigurationException("Unable to configure two-point control for component "
						+ "with missing temperature bondaries: " + heatingId);
			}
			return new TwoPointHeating(this, heating, temperature, 
					temperaturesMax.get(heatingId+"_temp_max"),
					temperaturesMin.get(heatingId+"_temp_min"));
		}
		logger.warn("Unable to find temperature configurations for heating: {}", heatingId);
		return super.onCreate(heating);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		temperatures.deregister();
	}

}
