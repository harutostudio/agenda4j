package io.agenda4j;

import io.agenda4j.core.CancelMode;
import io.agenda4j.core.CancelQuery;
import io.agenda4j.core.CancelResult;
import io.agenda4j.core.PersistResult;

import java.time.Instant;

/**
 * Main scheduler API.
 *
 * <p>Supports two scheduling styles:
 * <ul>
 *   <li>Absolute time scheduling (single run at a specific {@link Instant})</li>
 *   <li>Interval-based scheduling (human duration, cron string, or numeric seconds)</li>
 * </ul>
 */
public interface Agenda {
    void start();

    void stop();

    <T> JobBuilder<T> create(String name, T data);

    JobBuilder<Void> create(String name);

    /**
     * Schedule a one-time job at an absolute time.
     */
    <T> JobBuilder<T> schedule(String name, Instant time, T data);

    JobBuilder<Void> schedule(String name, Instant time);

    /**
     * Create or update a repeating job with string-based interval input.
     * Supported values include human duration text or cron expressions.
     */
    <T> PersistResult every(String name, String interval, T data, JobBuilder.RepeatOptions options);

    PersistResult every(String name, String interval, JobBuilder.RepeatOptions options);

    <T> PersistResult every(String name, Number interval, T data, JobBuilder.RepeatOptions options);

    PersistResult every(String name, Number interval, JobBuilder.RepeatOptions options);

    <T> PersistResult now(String name, T data);

    PersistResult now(String name);

    CancelResult cancel(CancelQuery query);

    CancelResult cancel(CancelQuery query, CancelOptions options);

    record CancelOptions(CancelMode mode, int limit) {
        public static CancelOptions defaults() {
            return new CancelOptions(CancelMode.DISABLE, Integer.MAX_VALUE);
        }
    }
}
