package org.the.cmpt.hp.basic;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.cmpt.hp.HeatPumpComponent;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

@Component(service = { HeatPumpService.class })
public class BasicComponent extends HeatPumpComponent {
	private final static Logger logger = LoggerFactory.getLogger(BasicComponent.class);

	private final static String ID = "Basic";

	private double temperatureMin;
	private double temperatureMax;
	private double temperatureInMax;

	private ChannelListener temperatureListener;
	private ChannelListener stateListener;
	private Channel state;

	private Value temperatureValue = DoubleValue.emptyValue();
	private Value stateValueLast = null;
	private volatile long startTimeLast;
	private int intervalMin;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		try {
			BasicConfig config = configs.getSection(BasicConfig.class);
			this.temperatureMin = config.getTemperatureMin();
			this.temperatureMax = config.getTemperatureMax();
			this.temperatureInMax = config.getInputTemperatureMax();
			this.temperatureListener = registerTemperatureListener(
					manager.getChannel(config.getTemperatureKey()));
			
			this.state = manager.getChannel(config.getStateKey());
			this.stateListener = registerStateListener(state);
			this.startTimeLast = System.currentTimeMillis();
			this.intervalMin = config.getIntervalMin();
			
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid heat pump configuration: " + e.getMessage());
		}
	}

	@Override
	public void onDeactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		if (temperatureListener != null) {
			temperatureListener.deregister();
		}
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value.booleanValue() && temperatureValue.doubleValue() > temperatureInMax) {
					logger.warn("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
					stop();
				}
				if (value.booleanValue() && stateValueLast != null && !stateValueLast.booleanValue()) {
					startTimeLast = value.getTime();
				}
				stateValueLast = value;
			}
		};
		return listener;
	}

	private ChannelListener registerTemperatureListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				temperatureValue = value;
				
				if (temperatureValue.doubleValue() >= temperatureInMax) {
					stop();
					return;
				}
				if (isMaintenance()) {
					return;
				}
				if (temperatureValue.doubleValue() <= temperatureMin &&
						(stateValueLast != null && !stateValueLast.booleanValue())) {
					
					start();
				}
				else if (temperatureValue.doubleValue() >= temperatureMax &&
						(stateValueLast != null && stateValueLast.booleanValue())) {
					if (System.currentTimeMillis() - startTimeLast < intervalMin) {
						logger.debug("Heat pump recognized temperature threshold to switch OFF while running shorter than {}min", intervalMin/60000);
						return;
					}
					stop();
				}
			}
		};
		return listener;
	}

	@Override
	public double getMaxPower() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinPower() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Value getCoefficientOfPerformance() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getElectricalEnergy() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getThermalEnergy() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getElectricalPower() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getThermalPower() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start(ComponentWriteContainer container, Value value) throws ComponentException {
		if (temperatureValue.doubleValue() >= temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
		container.add(state, new BooleanValue(true, value.getTime()));
	}

	@Override
	public void start(Value value) throws EnergyManagementException {
		if (temperatureValue.doubleValue() >= temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
		start(value.getTime());
	}

	protected void start(long time) {
		state.write(new BooleanValue(true, time));
	}

	protected void start() {
		start(System.currentTimeMillis());
	}

	@Override
	public void stop(ComponentWriteContainer container, Long time) throws ComponentException {
		container.add(state, new BooleanValue(false, time));
	}

	@Override
	public void stop(Long time) throws EnergyManagementException {
		stop(time);
	}

	protected void stop(long time) {
		state.write(new BooleanValue(false, time));
	}

	protected void stop() {
		stop(System.currentTimeMillis());
	}

}
