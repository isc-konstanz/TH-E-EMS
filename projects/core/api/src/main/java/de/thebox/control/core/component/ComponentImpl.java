package de.thebox.control.core.component;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public abstract class ComponentImpl implements ComponentService, ComponentCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(ComponentImpl.class);

	protected ComponentStatus status = ComponentStatus.DISABLED;
	protected ControlService control;

	@Override
	public ComponentStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(ComponentStatus status) throws ControlException {
		switch(status) {
		case MAINTENANCE:
			if (this.status != ComponentStatus.MAINTENANCE) {
				maintenance(true);
			}
			break;
		default:
			if (this.status == ComponentStatus.MAINTENANCE) {
				maintenance(false);
			}
			break;
		}
		this.status = status;
	}

	public void bind(ControlService context) throws ControlException {
		this.control = context;
		activate(load());
	}

	protected Preferences load() throws ConfigurationException {
		try {
			return control.readComponentConfigs(getId());
			
		} catch (IOException e) {
			// TODO: Fatal error! Inform error event handler
			throw new ConfigurationException("Error while reading component configuration: " + e.getMessage());
		}
	}

	@Override
	public void reload() throws ControlException {
		deactivate();
		activate(load());
	}

	protected abstract void maintenance(boolean enabled) throws ControlException;

	@Override
	public void set(Value value) throws ControlException {
		ComponentWriteContainer container = new ComponentWriteContainer();
		try {
			build(container, value);
			
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		}
		if (container.size() < 1) {
			return;
		}
		
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

	@Override
	public void schedule(Schedule schedule) throws ControlException {
		ComponentWriteContainer container = new ComponentWriteContainer();
		try {
			for (Value value : schedule) {
				build(container, value);
			}
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		}

		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

	protected abstract void build(ComponentWriteContainer container, Value value) throws ControlException;

}
