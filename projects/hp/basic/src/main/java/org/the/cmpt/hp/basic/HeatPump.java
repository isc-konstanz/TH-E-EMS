package org.the.cmpt.hp.basic;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.cmpt.hp.HeatPumpComponent;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

@Component(service = HeatPumpService.class)
public class HeatPump extends HeatPumpComponent {
	private final static Logger logger = LoggerFactory.getLogger(HeatPump.class);

	private final static String ID = "Basic";

	@Configuration(mandatory=false, scale=60000) // Default interval minimum of 10 minutes
	private int intervalMin = 600000;

	@Configuration("temp_min")
	private double temperatureMin;

	@Configuration("temp_max")
	private double temperatureMax;

	@Configuration("temp_in_max")
	private double temperatureInMax;

	@Configuration("temp_in")
	private ChannelListener temperature;

	private Value temperatureValue = DoubleValue.emptyValue();

	@Configuration
	private ChannelListener state;

	private Value stateValueLast = null;
	private volatile long startTimeLast;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);

		state.registerValueListener(new StateListener());
		temperature.registerValueListener(new TemperatureListener());
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
		super.onDeactivate();
		
		state.deregister();
		temperature.deregister();
	}

	@Override
	protected void onStart(ComponentWriteContainer container, Value value) throws ComponentException {
		if (temperatureValue.doubleValue() >= temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
		container.add(state, new BooleanValue(true, value.getTime()));
	}

	protected void onStart(long time) {
		state.write(new BooleanValue(true, time));
	}

	protected void onStart() {
		onStart(System.currentTimeMillis());
	}

	@Override
	protected void onStop(ComponentWriteContainer container, Long time) throws ComponentException {
		container.add(state, new BooleanValue(false, time));
	}

	@Override
	public void onStop(Long time) throws EnergyManagementException {
		onStop(time);
	}

	protected void onStop(long time) {
		state.write(new BooleanValue(false, time));
	}

	protected void onStop() {
		onStop(System.currentTimeMillis());
	}

	private class StateListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (value.booleanValue() && temperatureValue.doubleValue() > temperatureInMax) {
				logger.warn("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
				onStop();
			}
			if (value.booleanValue() && stateValueLast != null && !stateValueLast.booleanValue()) {
				startTimeLast = value.getTime();
			}
			stateValueLast = value;
		}
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureValue = value;
			
			if (temperatureValue.doubleValue() >= temperatureInMax) {
				onStop();
				return;
			}
			if (isMaintenance()) {
				return;
			}
			if (temperatureValue.doubleValue() <= temperatureMin &&
					(stateValueLast != null && !stateValueLast.booleanValue())) {
				
				onStart();
			}
			else if (temperatureValue.doubleValue() >= temperatureMax &&
					(stateValueLast != null && stateValueLast.booleanValue())) {
				if (System.currentTimeMillis() - startTimeLast < intervalMin) {
					logger.debug("Heat pump recognized temperature threshold to switch OFF while running shorter than {}min", 
							intervalMin/60000);
					
					return;
				}
				onStop();
			}
		}
	}

}
