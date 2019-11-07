package org.the.ems.ctrl.sd;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;
import org.the.ems.ctrl.tp.TwoPointControl;

@Component(
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".sd",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SurplusDiversionControl extends TwoPointControl {

	@Configuration(mandatory=false, scale=1000)
	protected double surplus = Double.POSITIVE_INFINITY;

	@Configuration(mandatory=false, scale=1000)
	protected double shortage = 0;

	@Configuration
	protected ChannelListener power;

	@Configuration(mandatory=false)
	protected double powerScale = 1;

	protected double powerValue = Double.NaN;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		if (surplus == Double.POSITIVE_INFINITY && component instanceof HeatingService) {
			surplus = ((HeatingService) component).getMinPower();
		}
		power.registerValueListener(new PowerListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		power.deregister();
	}

	@Override
	protected boolean checkStart(HeatingService heating) {
		if (checkMinTemperature()) {
			return true;
		}
		else if (checkMaxTemperature()) {
			return powerValue >= surplus;
		}
		return false;
	}

	@Override
	protected boolean checkStop(HeatingService heating) {
		if (checkMaxTemperature()) {
			return true;
		}
		else if (checkMinTemperature()) {
			return powerValue <= shortage;
		}
		return false;
	}

	@Override
	protected Value getValue(HeatingService heating) {
		double value = Math.max(
			Math.min(powerValue, heating.getMaxPower()), heating.getMinPower()
		);
		return new DoubleValue(value);
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			powerValue = value.doubleValue()*powerScale;
			onControlHeating();
		}
	}

}
