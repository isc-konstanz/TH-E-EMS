package de.thebox.control.core.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Scheduler {
	protected final ScheduledExecutorService scheduler;

	protected Schedule schedule;

	public Scheduler(String name) {
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E Component " + name + " - thread-");
		scheduler = Executors.newScheduledThreadPool(1, namedThreadFactory);
	}

	public void schedule(Schedule schedule) {
		this.schedule = schedule;
	}
}
