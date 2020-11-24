package org.the.ems.ctrl.ps;

import static java.lang.Math.abs;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
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
	protected double exportMax = Double.NEGATIVE_INFINITY;

	@Configuration(mandatory=false, scale=1000)
	protected double exportHyst = 0;


	@Configuration(mandatory=false, scale=1000)
	protected double importMax = 0;

//	@Configuration(mandatory=false, scale=1000)
//	protected double importMin = Double.NEGATIVE_INFINITY;
//
//	@Configuration(mandatory=false, scale=-1000)
//	protected double importHyst = 0;


	@Configuration(mandatory=false, section="PID", value="proportional")
	protected double controlProportional = 0.2;

	@Configuration(mandatory=false, section="PID", value="integral")
	protected double controlIntegral = 0.04;

	@Configuration(mandatory=false, section="PID", value="derivative")
	protected double controlDerivative = 0;

	@Configuration(mandatory=false, section="PID", value="tolerance")
	protected double controlTolerance = 10;

	@Configuration(mandatory=false, section="PID", value="maximum")
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

	protected Value powerValue = DoubleValue.emptyValue();

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		power.registerValueListener(new PowerListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		power.deregister();
	}

	protected void set(Value value) {
		double power = value.doubleValue();
//		long seconds = (value.getTime() - powerValue.getTime())/1000;
//		if (seconds < 1) {
//			seconds = 1;
//		}
		double error = 0;
		
		if (power <= exportMax) {
			error = exportMax - power;
			logger.trace("Power export above {}: {}", abs(exportMax), power);
		}
		if (power > importMax) {
			error = importMax - power;
			logger.trace("Power import above {}: {}", abs(importMax), power);
		}
		
		if (error != 0 &&
				(controlValue < 0 && power <= exportMax) || 
				(controlValue > 0 && power > importMax)) {
			
			controlValue = 0;
			controlError = 0;
			controlErrorIntegral = 0;
			
			logger.debug("Power boundary infringement changed extreme. Control values will be resetted.");
		}
		else if (controlValue > 0) {
			error = exportMax - power;
			logger.trace("Power export error from {}: {}", abs(exportMax), error);
		}
		else if (controlValue < 0) {
			error = importMax - power;
			logger.trace("Power import error from {}: {}", abs(importMax), error);
		}
//		double controlErrorProportional = error*controlProportional;
//		double controlErrorDerivate = (error - controlError)/seconds*controlDerivative;
//		controlErrorIntegral += error*seconds*controlIntegral;
//		controlError = error;
		
		controlErrorIntegral += error;
		
		if (controlErrorIntegral > controlMax) {
			controlErrorIntegral = controlMax;
		}
		if (controlErrorIntegral < -controlMax) {
			controlErrorIntegral = -controlMax;
		}
//		TODO: Verify PID control functionality and uncomment again together with commented lines above
//		double controlValue = controlErrorProportional + controlErrorIntegral + controlErrorDerivate;
		double controlValue = controlErrorIntegral;
		if ((abs(this.controlValue) < controlTolerance && 
				abs(controlValue) < controlTolerance) ||
				(this.controlValue > 0 && controlValue < 0) ||
				(this.controlValue < 0 && controlValue > 0)) {
			
			controlValue = 0;
			controlError = 0;
			controlErrorIntegral = 0;
			
			if (this.controlValue != 0) {
				logger.debug("Power boundary infringement resolved.");
			}
		}
		else if (controlValue > controlMax) {
			controlValue = controlMax;
		}
		else if (controlValue < -controlMax) {
			controlValue = -controlMax;
		}
		this.controlValue = controlValue;
		if (controlValue != 0) {
			logger.trace("Power control value update: {}", controlValue);
		}
		this.inverters.set(new DoubleValue(controlValue, value.getTime()));
	}

	protected void onPowerChanged(Value value) {
		double power = value.doubleValue();
		
		if ((power > importMax || power > exportMax + exportHyst) &&
				heatings.hasStoppable(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP)) {
			
			heatings.stopFirst(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP);
			logger.debug("Stopping heating due to power boundary infringement: {}", power);
		}
		else if (power <= exportMax && 
				power + heatings.getStartableMinPower(ComponentType.HEAT_PUMP, ComponentType.HEATING_ROD) <= exportMax + exportHyst) {
			
			heatings.startFirst(ComponentType.HEAT_PUMP, ComponentType.HEATING_ROD);
			logger.debug("Starting heating due to power boundary infringement: {}", power);
		}
		else {
			set(value);
		}
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.trace("Received power value: {}W", value);
			
			Value power = new DoubleValue(value.doubleValue()*powerScale, value.getTime());
			if (power.doubleValue() != powerValue.doubleValue()) {
				// Only update PID control if the power value changed.
				// Measurements will most probably always differ and only values read via a SCADA
				// system which are no new measurements can be expected to be exactly the same.
				onPowerChanged(power);
			}
			powerValue = power;
		}
	}

}