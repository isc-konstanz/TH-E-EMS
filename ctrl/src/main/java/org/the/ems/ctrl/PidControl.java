/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Configurable;
import org.the.ems.core.config.Configuration;

public class PidControl extends Configurable {
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(PidControl.class);
	
	private final static String SECTION = "PID";

	@Configuration(mandatory=false, value="proportional")
	protected double controlProportional = 0.2;

	@Configuration(mandatory=false, value="integral")
	protected double controlIntegral = 0.02;

	@Configuration(mandatory=false, value="derivative")
	protected double controlDerivative = 0.02;

	@Configuration(mandatory=false, value="tolerance")
	protected double controlTolerance = 10;

	@Configuration(mandatory=false, value="maximum")
	protected double controlMax = 10000;

	@Configuration(mandatory=false, value="minimum")
	protected double controlMin = 0;

	protected double errorPrev = 0;

	protected double proportional = 0;
	protected double integral = 0;
	protected double derivative = 0;
	protected double pid = 0;

	protected long  timeLast = System.currentTimeMillis();

	public PidControl() {
		super(SECTION);
	}

	public double process(long time, double error) {
		double dt = (time - timeLast)/1000;
		if (error > controlMax ) {
			error = controlMax;
		} else if (error < -controlMax) {
			error = -controlMax;
		}
		
		proportional = controlProportional * error;
		integral = Math.min(controlMin, integral + controlIntegral * error * dt);
		integral = Math.max(-controlMax, integral);
		derivative = controlDerivative * (error - errorPrev) / dt;
		
		pid = Math.max(proportional + integral + derivative, -controlMax);
		pid = Math.min(pid, controlMin);
		
		errorPrev = error;
		timeLast = time;
		
		return pid;
	}

}
