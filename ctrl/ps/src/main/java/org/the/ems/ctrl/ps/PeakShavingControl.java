package org.the.ems.ctrl.ps;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;
import org.the.ems.ctrl.tp.TwoPointControl;

@Component(
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".ps",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class PeakShavingControl extends TwoPointControl {
	private final static Logger logger = LoggerFactory.getLogger(PeakShavingControl.class);

	@Configuration(mandatory=false, scale=-1000)
	protected double exportSurplus = Double.NEGATIVE_INFINITY;

	@Configuration(mandatory=false, scale=-1000)
	protected double exportMax = Double.NEGATIVE_INFINITY;

	@Configuration(mandatory=false, scale=1000)
	protected double importMax = 0;

	@Configuration(mandatory=false, section="PID", value="proportional")
	protected double controlProportional = 0.15;

	@Configuration(mandatory=false, section="PID", value="integral")
	protected double controlIntegral = 0.015;

	@Configuration(mandatory=false, section="PID", value="derivative")
	protected double controlDerivative = 1;

	protected double controlMax = Double.POSITIVE_INFINITY;
	protected double controlErrorIntegral = 0;
	protected double controlError = 0;
	protected double controlValue = 0;

	@Configuration(mandatory=false)
	protected Channel setpoint;

	@Configuration
	protected ChannelListener power;

	@Configuration(mandatory=false)
	protected double powerScale = 1;

	@Configuration(mandatory=false, scale=1000)
	protected double powerTolerance = 0;

	protected Value powerValue = DoubleValue.emptyValue();

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
//		if (exportMax == Double.NEGATIVE_INFINITY && components.contains(HeatingService.class)) {
//			for (HeatingService component : components.getAll(HeatingService.class)) {
//				exportMax = Math.max(exportMax, -component.getMinPower());
//			}
//		}
		power.registerValueListener(new PowerListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		power.deregister();
	}

	protected void onPowerChanged(Value value) {
		double power = value.doubleValue();
		if (power < exportMax || power > importMax || controlValue != 0) {
			onControlChanged(value);
		}
		if ((power <= exportMax || power <= exportSurplus) && !isMaxTemperature()) {
			start();
		}
		else if ((power > importMax || power > powerTolerance) && !isMinTemperature()) {
			stop();
		}
	}

	protected void onControlChanged(Value value) {
		long seconds = (value.getTime() - powerValue.getTime())/1000;
		double power = value.doubleValue();
		double error = 0;
		if (power < exportMax || controlValue > 0) {
			error = exportMax - power;
		}
		if (power > importMax || controlValue < 0) {
			error = importMax - power;
		}
		double controlErrorDerivate = (error - controlError)/seconds*controlDerivative;
		controlErrorIntegral += error*seconds*controlIntegral;
		controlError = error;
		
		if (controlErrorIntegral > controlMax) {
			controlErrorIntegral = controlMax;
		}
		if (controlErrorIntegral < -controlMax) {
			controlErrorIntegral = -controlMax;
		}
		double controlValue = controlProportional*controlError + controlErrorIntegral + controlErrorDerivate;
		if ((controlValue == 0 && this.controlValue != 0) ||
				(controlValue > 0 && this.controlValue < 0) ||
				(controlValue < 0 && this.controlValue > 0)) {
			
			controlErrorIntegral = 0;
			controlError = 0;
			controlValue = 0;
			return;
		}
		else if (controlValue > controlMax) {
			controlValue = controlMax;
		}
		else if (controlValue < -controlMax) {
			controlValue = -controlMax;
		}
		this.controlValue = controlValue;
		set(controlValue);
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.debug("Received power value: {}W", value);
			
			double power = value.doubleValue()*powerScale;
			if (powerValue.doubleValue() != power) {
				onPowerChanged(value);
			}
			powerValue = new DoubleValue(power, value.getTime());
		}
	}

}
