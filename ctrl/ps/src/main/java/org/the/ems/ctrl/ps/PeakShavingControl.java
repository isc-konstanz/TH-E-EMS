package org.the.ems.ctrl.ps;

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

	@Configuration(mandatory=false, scale=-1000)
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
	protected double controlTolerance = 50;

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
		long seconds = (value.getTime() - powerValue.getTime())/1000;
		if (seconds < 1) {
			seconds = 1;
		}
		double error = 0;
		
		if (power <= exportMax) {
			error = exportMax - power;
		}
		if (power > importMax) {
			error = importMax - power;
		}
		
		if (error != 0 &&
				(controlValue < 0 && power <= exportMax) || 
				(controlValue > 0 && power > importMax)) {
			
			controlValue = 0;
			controlError = 0;
			controlErrorIntegral = 0;
		}
		else if (controlValue > 0) {
			error = exportMax - power;
		}
		else if (controlValue < 0) {
			error = importMax - power;
		}
		
		double controlErrorProportional = error*controlProportional;
		double controlErrorDerivate = (error - controlError)/seconds*controlDerivative;
		controlErrorIntegral += error*seconds*controlIntegral;
		controlError = error;
		
		if (controlErrorIntegral > controlMax) {
			controlErrorIntegral = controlMax;
		}
		if (controlErrorIntegral < -controlMax) {
			controlErrorIntegral = -controlMax;
		}
		double controlValue = controlErrorProportional + controlErrorIntegral + controlErrorDerivate;
		if (Math.abs(this.controlValue) < controlTolerance && 
				Math.abs(controlValue) < controlTolerance &&
				(this.controlValue > 0 && controlValue < 0) ||
				(this.controlValue < 0 && controlValue > 0)) {
			
			controlValue = 0;
			controlError = 0;
			controlErrorIntegral = 0;
		}
		if (controlValue > controlMax) {
			controlValue = controlMax;
		}
		else if (controlValue < -controlMax) {
			controlValue = -controlMax;
		}
		this.controlValue = controlValue;
		
		this.inverters.set(
				new DoubleValue(controlValue, value.getTime()));
	}

	protected void onPowerChanged(Value value) {
		double power = value.doubleValue();
		
		if ((power > importMax && powerValue.doubleValue() <= importMax) || 
				(power > exportMax + exportHyst &&
				heatings.hasStoppable(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP))) {
			
			heatings.stopFirst(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP);
		}
		else if (power <= exportMax && powerValue.doubleValue() <= exportMax &&
				heatings.hasStartable(ComponentType.HEAT_PUMP, ComponentType.HEATING_ROD)) {
			
			heatings.startFirst(ComponentType.HEAT_PUMP, ComponentType.HEATING_ROD);
		}
		else if (power <= exportMax || power > importMax || controlValue != 0) {
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
