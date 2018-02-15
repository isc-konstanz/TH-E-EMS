package de.thebox.control.opt.service;

import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public class GridPowerListener implements ValueListener {

	/**
	 * Interface used to notify the {@link GridService} 
	 * implementation about changed power values
	 */
	public interface GridPowerCallbacks {
		public void onGridPowerReceived(GridPowerType type, Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed power values
	 */
	private final GridPowerCallbacks callbacks;

	private final GridPowerType type;

	public GridPowerListener(GridPowerCallbacks callbacks, GridPowerType type) {
		this.callbacks = callbacks;
		this.type = type;
	}

	public GridPowerType getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		if (value != null) {
			callbacks.onGridPowerReceived(type, value);
		}
	}
}
