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

	private long rotationTimeSetpoint = -1;


	@Configuration(value = "pos_angle_min", mandatory = false)
	private int positionAngleMin = 0;

	@Configuration(value = "pos_angle_max", mandatory = false)
	private int positionAngleMax = 90;

	@Configuration(value = "pos_angle_setpoint")
	private Channel positionAngleSetpoint;

	@Configuration(value = "pos_angle", mandatory = false)
	private Channel positionAngle;

	private DoubleValue positionAngleValue = DoubleValue.emptyValue();
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

	public int getPositionAngleMinimum() {
		return positionAngleMin;
	}

	public int getPositionAngleMaximum() {
		return positionAngleMax;
	}

	public Value getPositionAngle() {
		return positionAngleValue;
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

	protected void calibrate() throws EnergyManagementException {
		logger.debug("Calibration initiated");
		
		long rotationTimeStart = System.currentTimeMillis();
		rotationTimeSetpoint = rotationTimeStart + rotateDuration;
		positionAngleValue = new DoubleValue(0, rotationTimeStart);
		if (positionAngle != null) {
			positionAngle.setLatestValue(positionAngleValue);
		}
		positionAngleCalibrated = false;
		
		WriteContainer writeContainer = new WriteContainer();
		doRotateMin(writeContainer);
		write(writeContainer);
		startWatcher();
	}

	private void doRotateMin(WriteContainer container) throws EnergyManagementException {
		switch (rotateMin) {
		case CW:
			if (isRotatingCounterClockwise()) {
				container.addBoolean(rotateCounterClockwise, false);
			}
			container.addBoolean(rotateClockwise, true);
			break;
		case CCW:
		default:
			if (isRotatingClockwise()) {
				container.addBoolean(rotateClockwise, false);
			}
			container.addBoolean(rotateCounterClockwise, true);
			break;
		}
	}

	private void doRotateMax(WriteContainer container) throws EnergyManagementException {
		switch (rotateMin) {
		case CW:
			if (isRotatingClockwise()) {
				container.addBoolean(rotateClockwise, false);
			}
			container.addBoolean(rotateCounterClockwise, true);
			break;
		case CCW:
		default:
			if (isRotatingCounterClockwise()) {
				container.addBoolean(rotateCounterClockwise, false);
			}
			container.addBoolean(rotateClockwise, true);
			break;
		}
	}

	public final void set(Value value) throws EnergyManagementException {
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		try {
			if (Math.abs(value.doubleValue() - 
					positionAngleSetpoint.getLatestValue().doubleValue()) > .1) {
				this.positionAngleSetpoint.setLatestValue(value);
				return;
			}
		} catch (InvalidValueException e) {
			// Setpoint angle channel not yet set
			this.positionAngleSetpoint.setLatestValue(value);
			return;
		}
		doSet(value);
	}

	synchronized void doSet(Value value) throws EnergyManagementException {
		WriteContainer container = new WriteContainer();
		onSet(container, value);
		write(container);
		startWatcher();
	}

	protected synchronized void onSet(WriteContainer container, Value value) throws EnergyManagementException {
        if (!positionAngleCalibrated) {
            logger.debug("Skipping setting of rotation angle setpoint while calibrating");
            return;
        }
		logger.debug("Received position angle setpoint value: {}", value.doubleValue());
		
        double positionAngleDelta = Math.abs(value.doubleValue() - positionAngleValue.doubleValue());
		long rotationTimeDelta = Math.round(rotateDuration*(positionAngleDelta / (double) getPositionAngleMaximum()));
		long rotationTimeStart = System.currentTimeMillis();
		rotationTimeSetpoint = rotationTimeStart + rotationTimeDelta;
		
		// Set angle value, as timestamp will be used to calculate passed time
		double positionAngle = positionAngleValue.doubleValue();
		positionAngleValue = new DoubleValue(positionAngle, rotationTimeStart);
		
		logger.debug("Calculated necessary rotation time of {} seconds", rotationTimeDelta/1000);
		
		if (value.doubleValue() < positionAngleValue.doubleValue()) {
			doRotateMin(container);
		}
		else if (value.doubleValue() > positionAngleValue.doubleValue()) {
			doRotateMax(container);
		}
	}

	@Override
	public void onValueChanged(Value value) {
		try {
			doSet(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Error setting valve position angle setpoint: {}", e.getMessage());
		}
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		super.onConfigure(configs);
		
		int millisPerDegree = (int) Math.round(((double) rotateDuration)/(positionAngleMax*10.));
		if (millisPerDegree < 100) {
			millisPerDegree = 100;
		}
		positionAngleWatchInterval = millisPerDegree;
		
		logger.debug("Configure position angle watcher interval of {}ms", positionAngleWatchInterval);
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		try {
			positionAngleSetpoint.setLatestValue(DoubleValue.zeroValue());
			positionAngleSetpoint.registerValueListener(this);
			calibrate();
			
		} catch (EnergyManagementException e) {
			logger.error("Error calibrating valve position angle: {}", e.getMessage());
		}
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.stopWatcher();
	}

	private void startWatcher() {
		if (positionAngleWatcher != null) {
			synchronized (positionAngleWatcher) {
				if (!positionAngleWatcher.isInterrupted() && 
						positionAngleWatcher.isAlive()) {
					return;
				}
			}
		}
		positionAngleWatcher = new Thread(this);
		positionAngleWatcher.setName("TH-E EMS "+getId().toUpperCase()+" position angle watcher");
		positionAngleWatcher.start();
	}

	private void stopWatcher() {
		deactivate = true;
		
		synchronized (positionAngleWatcher) {
			positionAngleWatcher.interrupt();
			try {
				positionAngleWatcher.join();
				
			} catch (InterruptedException e) {
			}
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
							double rotationTimeChange = (timestamp - positionAngleValue.getEpochMillis()) / (double) rotateDuration;
							double positionAngle = positionAngleValue.doubleValue();
							double positionAngleChange = (getPositionAngleMaximum() - getPositionAngleMinimum()) * 
									rotationTimeChange;
							
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
							positionAngleValue = new DoubleValue(positionAngle, timestamp);
							
							if (timestamp >= rotationTimeSetpoint) {
								reset();
							}
						}
					}
					else if (timestamp >= rotationTimeSetpoint) {
						positionAngleCalibrated = true;
						reset();
						logger.debug("Calibration complete");
					}
					if (positionAngleValue.isFinite() && positionAngle != null &&
							positionAngle.getLatestValue().doubleValue() != positionAngleValue.doubleValue()) {
						positionAngle.setLatestValue(positionAngleValue);
						logger.debug("Calculate new position angle: {}", positionAngleValue.doubleValue());
					}
					if (!isRotating()) {
						if (Math.abs(positionAngleValue.doubleValue() -
								positionAngleSetpoint.getLatestValue().doubleValue()) > .1) {
							set(positionAngleSetpoint.getLatestValue());
						}
						else {
							deactivate = true;
							logger.debug("Deactivate position angle watcher for rotation task is complete");
						}
					}
				} catch (InvalidValueException e) {
					logger.debug("Unable to retrieve valve position angle: {}", e.getMessage());
					
				} catch (EnergyManagementException e) {
					logger.error("Error setting valve rotation: {}", e.getMessage());
				}
				if (!deactivate) {
					long time = System.currentTimeMillis() - timestamp;
					long sleep = positionAngleWatchInterval - time;
					if (sleep > 0) {
						Thread.sleep(sleep);
					}
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while watching valve position angle");
			}
		}
	}

}