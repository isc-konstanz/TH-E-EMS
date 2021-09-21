/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
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
package org.the.ems.core.data;

import org.the.ems.core.EnergyManagementException;

public class InvalidValueException extends EnergyManagementException {
	private static final long serialVersionUID = 3548783014820967687L;

	public static enum Severity {
		INFO, WARNING, ERROR;
	}

	private final Severity severity;
	private final Channel source;

	public InvalidValueException(Channel source, Severity severity) {
		super();
		this.severity = severity;
		this.source = source;
	}

	public InvalidValueException(Channel source, Severity severity, String str) {
		super(str);
		this.severity = severity;
		this.source = source;
	}

	public InvalidValueException(Channel source, Severity severity, Throwable cause) {
		super(cause);
		this.severity = severity;
		this.source = source;
	}

	public Severity getSeverity() {
		return severity;
	}

	public Channel getSource() {
		return source;
	}

}
