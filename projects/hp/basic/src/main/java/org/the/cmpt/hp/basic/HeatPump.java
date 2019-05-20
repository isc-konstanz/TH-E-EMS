package org.the.cmpt.hp.basic;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
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
import org.the.ems.core.data.WriteContainer;

@Component(service = HeatPumpService.class)
public class HeatPump extends HeatPumpComponent {
	private final static Logger logger = LoggerFactory.getLogger(HeatPump.class);

	private final static String ID = "Basic";

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

	protected Value stateValueLast = null;

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
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		if (temperatureValue.doubleValue() > temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
		container.add(state, new BooleanValue(true, value.getTime()));
	}

	@Override
	protected void onStop(WriteContainer container, Long time) throws ComponentException {
		container.add(state, new BooleanValue(false, time));
	}

	private class StateListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (value.booleanValue() && temperatureValue.doubleValue() > temperatureInMax) {
				logger.warn("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
				// TODO: implement virtual start signal that does not affect relay
				state.write(new BooleanValue(false, value.getTime()));
				return;
			}
			else if (value.booleanValue() && stateValueLast != null && !stateValueLast.booleanValue()) {
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
				state.write(new BooleanValue(false, value.getTime()));;
				return;
			}
			if (isMaintenance()) {
				return;
			}
			if (temperatureValue.doubleValue() <= temperatureMin &&
					(stateValueLast != null && !stateValueLast.booleanValue())) {
				
				state.write(new BooleanValue(true, value.getTime()));
			}
			else if (temperatureValue.doubleValue() >= temperatureMax &&
					(stateValueLast != null && stateValueLast.booleanValue())) {
				
				state.write(new BooleanValue(false, value.getTime()));
			}
		}
	}

}
