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
package org.the.ems.cmpt.vlv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.IntValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

public class Valve extends Component implements Runnable, ValueListener {

	private static final Logger logger = LoggerFactory.getLogger(Valve.class);

	private final static String SECTION = "Valve";

	private enum Rotation {
		CW, CCW;
	}

	@Configuration(value = "rotate_cw")
	private Channel rotateClockwise;

	@Configuration(value = "rotate_ccw")
	private Channel rotateCounterClockwise;

	@Configuration(value = "rotate_duration", scale = 1000)
	private int rotateDuration;

	@Configuration(value = "rotate_min", mandatory = false)
	Rotation rotateMin = Rotation.CCW;


//	@Configuration(value = "pos_angle_min", mandatory = false)
//	private int positionAngleMin = 0;

	@Configuration(value = "pos_angle_max", mandatory = false)
	private int positionAngleMax = 90;

	@Configuration(value = "pos_angle_setpoint")
	private Channel positionAngleSetpoint;

	@Configuration(value = "pos_angle", mandatory = false)
	private Channel positionAngle;

	private DoubleValue positionAngleMinimum = DoubleValue.emptyValue();
	private boolean positionAngleCalibrated = false;

	private Thread positionAngleWatcher = null;
	private int positionAngleWatchInterval;

	private volatile boolean deactivate;


	public Valve(String section) {
		super(section);
	}

	public Valve() {
		super(SECTION);
	}

	private int getPositionAngleMinimum() {
		// TODO: Implement minimum angle
		//return positionAngleMin;
		return 0;
	}

	public int getPositionAngleMaximum() {
		return positionAngleMax;
	}

	public Value getPositionAngle() {
		return positionAngleMinimum;
	}

	public Value getPositionAngleSetpoint() throws InvalidValueException {
		return positionAngleSetpoint.getLatestValue();
	}

	private boolean isRotating() throws InvalidValueException {
		return isRotatingClockwise() || isRotatingCounterClockwise();
	}

	private boolean isRotatingClockwise() throws InvalidValueException {
		return rotateClockwise.getLatestValue().booleanValue();
	}

	private boolean isRotatingCounterClockwise() throws InvalidValueException {
		return rotateCounterClockwise.getLatestValue().booleanValue();
	}

	public void reset() throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		doReset();
	}

	void doReset() throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		onReset(writeContainer);
		write(writeContainer);
	}

	protected void onReset(WriteContainer container) throws EnergyManagementException {
		if (isRotatingClockwise()) {
			container.addBoolean(rotateClockwise, false);
		}
		if (isRotatingCounterClockwise()) {
			container.addBoolean(rotateCounterClockwise, false);
		}
	}

	public void setMin() throws EnergyManagementException {
		set(new IntValue(getPositionAngleMinimum()));
	}

	public void setMax() throws EnergyManagementException {
		set(new IntValue(getPositionAngleMaximum()));
	}

	public final void set(Value value) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		doSet(value);
	}

	void doSet(Value positionAngleValue) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		try {
			if (Math.abs(positionAngleValue.doubleValue() -
					positionAngleSetpoint.getLatestValue().doubleValue()) > .1) {
				writeContainer.add(positionAngleSetpoint, positionAngleValue);
			}
		} catch (InvalidValueException e) {
			// Setpoint channel not yet written to
			writeContainer.add(positionAngleSetpoint, positionAngleValue);
		}
		onSet(writeContainer, positionAngleValue);
		write(writeContainer);
	}

	protected void onSet(WriteContainer container, Value value) throws EnergyManagementException {
		if (!positionAngleCalibrated) {
			logger.debug("Skipping setting of rotation while calibrating");
			return;
		}
		// TODO: Implement {@link Rotation} configuration
		if (positionAngleMinimum.doubleValue() > value.doubleValue()) {
			positionAngleMinimum = new DoubleValue(positionAngleMinimum.doubleValue());
			if (isRotatingClockwise()) {
				container.addBoolean(rotateClockwise, false);
			}
			container.addBoolean(rotateCounterClockwise, true);
		}
		else if (positionAngleMinimum.doubleValue() < value.doubleValue()) {
			positionAngleMinimum = new DoubleValue(positionAngleMinimum.doubleValue());
			if (isRotatingCounterClockwise()) {
				container.addBoolean(rotateCounterClockwise, false);
			}
			container.addBoolean(rotateClockwise, true);
		}
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		super.onConfigure(configs);
		
		int millisPerDegree = (int) Math.round(((double) rotateDuration)/(double) positionAngleMax);
		if (millisPerDegree < 100) {
			millisPerDegree = 100;
		}
		positionAngleWatchInterval = millisPerDegree;
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		try {
			positionAngleMinimum = new DoubleValue(getPositionAngleMinimum());
			positionAngleCalibrated = false;
			positionAngleSetpoint.registerValueListener(this);
			set(positionAngleMinimum);
			
			// TODO: run this thread only if rotating
			positionAngleWatcher = new Thread(this);
			positionAngleWatcher.setName("TH-E EMS "+getId().toUpperCase()+" position angle watcher");
			positionAngleWatcher.start();
			
		} catch (EnergyManagementException e) {
			logger.error("Error calibrating valve position angle: {}", e.getMessage());
		}
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		deactivate = true;
		
		positionAngleWatcher.interrupt();
		try {
			positionAngleWatcher.join();
			
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void onValueChanged(Value value) {
		try {
			set(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Error setting valve position angle setpoint: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		deactivate = false;
		while (!deactivate) {
			try {
				long timestamp = System.currentTimeMillis();
				try {
					if (positionAngleCalibrated) {
						if (isRotating()) {
							double positionAngle = positionAngleMinimum.doubleValue();
							double positionAngleChange = (timestamp - positionAngleMinimum.getEpochMillis()) * 
									((double) positionAngleMax)/(double) rotateDuration;
							if (isRotatingClockwise())  {
								positionAngle += positionAngleChange;
							}
							else if (isRotatingCounterClockwise()) {
								positionAngle -= positionAngleChange;
							}
							positionAngle = Math.round(positionAngle*10)/10.;
							if (positionAngle > getPositionAngleMaximum()) {
								positionAngle = getPositionAngleMaximum();
							}
							if (positionAngle < getPositionAngleMinimum()) {
								positionAngle = getPositionAngleMinimum();
							}
							positionAngleMinimum = new DoubleValue(positionAngle);
							
							double positionAngleSetValue = positionAngleSetpoint.getLatestValue().doubleValue();
							if ((isRotatingCounterClockwise() && positionAngle <= positionAngleSetValue) || 
									(isRotatingClockwise() && positionAngle >= positionAngleSetValue)) {
								reset();
							}
						}
						else if (Math.abs(positionAngleMinimum.doubleValue() -
								positionAngleSetpoint.getLatestValue().doubleValue()) > .1) {
							set(positionAngleSetpoint.getLatestValue());
						}
					}
					// TODO: Implement {@link Rotation} configuration
					else if (timestamp - positionAngleMinimum.getEpochMillis() >= rotateDuration) {
						positionAngleCalibrated = true;
						reset();
					}
					if (positionAngleMinimum.isFinite() && positionAngle != null) {
						positionAngle.setLatestValue(positionAngleMinimum);
					}
				} catch (InvalidValueException e) {
					logger.debug("Unable to retrieve valve position angle: {}", e.getMessage());
					
				} catch (EnergyManagementException e) {
					logger.error("Error setting valve rotation: {}", e.getMessage());
				}
				
				long time = System.currentTimeMillis() - timestamp;
				long sleep = positionAngleWatchInterval - time;
				if (sleep > 0) {
					Thread.sleep(sleep);
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while watching valve position angle");
			}
		}
	}

}
