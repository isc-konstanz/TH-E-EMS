package de.thebox.control.opt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.feature.emoncms.Emoncms;
import de.thebox.control.opt.Optimization;
import de.thebox.control.opt.service.GridPowerListener.GridPowerCallbacks;


public class GridService implements GridPowerCallbacks {

	/**
	 * Interface used to notify the {@link Optimization} 
	 * implementation about received grid service requests
	 */
	public interface GridServiceCallbacks {
		public void onGridServiceRequest(Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of received grid service requests
	 */
	private final GridServiceCallbacks callbacks;

	private final Channel gridService;

	private final Emoncms emoncms;
	private final String exportPowerFeed;
	private final String importPowerFeed;
	protected long exportTimeLast = 0;
	protected long importTimeLast = 0;
	protected Value exportPowerLast = DoubleValue.emptyValue();
	protected Value importPowerLast = DoubleValue.emptyValue();
	protected final List<GridPowerListener> powerlisteners = new ArrayList<GridPowerListener>();

	public GridService(GridServiceCallbacks callbacks, ControlService control, Preferences prefs) throws ComponentException {
		this.callbacks = callbacks;
		
		GridServiceConfig config = new GridServiceConfig(prefs);
		try {
			gridService = control.getChannel(config.getGridService());
			
			emoncms = new Emoncms(prefs);
			
			exportPowerFeed = config.getGridExportFeed();
			registerGridPowerListener(exportPowerFeed, GridPowerType.EXPORT);
			
			importPowerFeed = config.getGridImportFeed();
			registerGridPowerListener(importPowerFeed, GridPowerType.IMPORT);
			
		} catch (ControlException e) {
			throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
		}
	}

	private void registerGridPowerListener(String id, GridPowerType type) throws ControlException {
		GridPowerListener listener = new GridPowerListener(this, type);
		emoncms.registerFeedListener(id, listener);
		powerlisteners.add(listener);
	}

	public void deactivate() {
		if (emoncms != null) {
			emoncms.deregisterFeedListener(exportPowerFeed);
			emoncms.deregisterFeedListener(importPowerFeed);
			emoncms.deactivate();
		}
	}

	@Override
	public void onGridPowerReceived(GridPowerType type, Value value) {
		switch(type) {
		case EXPORT:
			exportTimeLast = System.currentTimeMillis();
			exportPowerLast = value;
			break;
		case IMPORT:
			importTimeLast = System.currentTimeMillis();
			importPowerLast = value;
			break;
		}
		if (Math.abs(importTimeLast - exportTimeLast) <= 1000) {
			long time = Math.max(importPowerLast.getTimestamp(), exportPowerLast.getTimestamp());
			double power = importPowerLast.doubleValue() - exportPowerLast.doubleValue();
			Value powerValue = new DoubleValue(power, time);
			
			gridService.setLatestValue(powerValue);
			callbacks.onGridServiceRequest(powerValue);
		}
	}
}
