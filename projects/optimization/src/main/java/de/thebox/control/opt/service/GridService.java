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
import de.thebox.control.opt.service.GridPowerListener.GridPowerCallbacks;


public class GridService implements GridPowerCallbacks {

	private final Channel gridService;

	private final Emoncms emoncms;
	private final String exportPowerFeed;
	private final String importPowerFeed;
	protected Value exportPowerLast = DoubleValue.emptyValue();
	protected Value importPowerLast = DoubleValue.emptyValue();
	protected final List<GridPowerListener> powerlisteners = new ArrayList<GridPowerListener>();

	public GridService(ControlService control, Preferences prefs) throws ComponentException {
		GridServiceConfig config = new GridServiceConfig(prefs);
		try {
			gridService = control.getChannel(config.getGridService());
			
			emoncms = new Emoncms(prefs);
			
			exportPowerFeed = config.getGridImportFeed();
			registerGridPowerListener(exportPowerFeed, GridPowerType.EXPORT);
			
			importPowerFeed = config.getGridImportFeed();
			registerGridPowerListener(importPowerFeed, GridPowerType.IMPORT);
			
		} catch (ControlException e) {
			throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
		}
	}

	private void registerGridPowerListener(String id, GridPowerType type) {
		GridPowerListener listener = new GridPowerListener(this, type);
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
			exportPowerLast = value;
			break;
		case IMPORT:
			importPowerLast = value;
			break;
		}
		if (exportPowerLast.getTimestamp() == importPowerLast.getTimestamp()) {
			double power = importPowerLast.doubleValue() - exportPowerLast.doubleValue();
			gridService.setLatestValue(new DoubleValue(power, exportPowerLast.getTimestamp()));
		}
	}
}
