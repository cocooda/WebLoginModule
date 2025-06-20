package com.vifinancenews.common.utilities;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A generic scheduler utility for running recurring cleanup or background tasks.
 * This class is defined in the common library and accepts any Runnable task from the calling module.
 */
public class AccountDeletionScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Starts a scheduled background task.
     *
     * @param task          the cleanup or maintenance task to run
     * @param initialDelay  delay before first execution (in hours)
     * @param intervalHours interval between repeated executions (in hours)
     */
    public static void start(Runnable task, long initialDelay, long intervalHours) {
        scheduler.scheduleAtFixedRate(task, initialDelay, intervalHours, TimeUnit.HOURS);
        System.out.printf("Scheduled task started: initial delay = %d hour(s), interval = %d hour(s)%n",
                          initialDelay, intervalHours);
    }

    /**
     * Optionally shut down the scheduler gracefully.
     */
    public static void stop() {
        scheduler.shutdown();
        System.out.println("Scheduler stopped.");
    }
}
