package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentCallbacks;
import de.thebox.control.core.component.ComponentWriteContainer;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.ShortValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;

public class Battery {
	protected ChannelListener listener;

	protected Value voltageLast = ShortValue.emptyValue();
	
	private Channel mode;
	private Channel chargeCurrent;
	private Channel dischargeCurrent;
	private Channel voltage;

	private ComponentCallbacks callbacks;
	
	private double lastChargeCurrent = 0;
	private double lastDischargeCurrent = 0;
	private Mode lastMode = null;

	public final static short MIN_CURRENT = 10;
	public final static short MAX_CURRENT = 100;
	
	protected class VoltageListener extends ChannelListener {
		public VoltageListener(Channel channel) {
			super(channel);
		}

		@Override
		public void onValueReceived(Value voltage) {
			voltageLast = voltage;
			callbacks.onUpdate();
		}
	}
	
	public Battery(ComponentCallbacks callbacks, ControlService control, Preferences prefs) throws ConfigurationException {
		try {
			BatteryConfig config = new BatteryConfig(prefs);
			
			this.callbacks = callbacks;
			
			mode = control.getChannel(config.getMode());
			chargeCurrent = control.getChannel(config.getChargeCurrent());
			dischargeCurrent = control.getChannel(config.getDischargeCurrent());
			voltage = control.getChannel(config.getVoltage());
			
			listener = new VoltageListener(voltage);
			
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid consumption configuration: " + e.getMessage());
		}
	}
	
	public void deactivate() {
		listener.deregister();
	}
	
	public void setMode(ComponentWriteContainer container, Mode mode) {
		Value value;
		if (mode != lastMode) {
			if (mode == Mode.DISABLED) {
				value = new ShortValue((short) 0x2100);
				container.add(this.mode, value);
			}
			else if (mode == Mode.CHARGE_FROM_GRID) {
				value = new ShortValue((short) 0xC100);
				container.add(this.mode, value);
			}
			else if (mode == Mode.DISCHARGE_TO_GRID) {
				value = new ShortValue((short) 0x3F00);
				container.add(this.mode, value);
			}
			else if (mode == Mode.DEFAULT){ 
				value = new ShortValue((short) 0xB900);
				container.add(this.mode, value);
			}
			else {
				//Exception
			}
			
			lastMode = mode;
		}
	}
	
	public void setChargeCurrent(ComponentWriteContainer container, Value power) {
		Value current = new ShortValue((short) (power.shortValue() / voltageLast.shortValue()));

		if (lastChargeCurrent != current.shortValue()) {
			if (current.shortValue() < MAX_CURRENT) {
				container.add(chargeCurrent, current);
				lastChargeCurrent = current.shortValue();
			}
			else {
				current = new ShortValue(MAX_CURRENT);
				container.add(chargeCurrent, current);
				lastChargeCurrent = current.shortValue();
			}
		}
	}

	public void setDischargeCurrent(ComponentWriteContainer container, Value power) {
		Value current = new ShortValue((short) (power.shortValue() / voltageLast.shortValue()));
		
		if (lastDischargeCurrent != current.shortValue()) {
			if (current.shortValue() < MAX_CURRENT){
				container.add(dischargeCurrent, current);
				lastDischargeCurrent = current.shortValue();
			}
			else {
				current = new ShortValue(MAX_CURRENT);
				container.add(dischargeCurrent, current);
				lastDischargeCurrent = current.shortValue();
			}		
		}
	}
	
	public int getMinimumPower() {
		int power = MIN_CURRENT * voltageLast.shortValue();
		return power;
	}
}
