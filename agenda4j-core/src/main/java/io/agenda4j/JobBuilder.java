package io.agenda4j;

import io.agenda4j.core.JobSpec;
import io.agenda4j.core.PersistResult;
import io.agenda4j.core.Priority;

import java.time.Instant;
import java.util.Map;

/**
 * Fluent builder for configuring a job before persisting it.
 *
 * <p>Note:
 * <ul>
 *   <li>build(): returns an in-memory job spec</li>
 *   <li>save(): build() + upsert to MongoDB</li>
 * </ul>
 */
public interface JobBuilder<T> {

    /**
     * Options for repeat scheduling.
     * <ul>
     *   <li>skipImmediate: if true, do not run immediately; schedule from the next computed run time</li>
     *   <li>timezone: IANA time zone id (e.g. "Asia/Taipei"); null means system default</li>
     * </ul>
     */
    record RepeatOptions(boolean skipImmediate, String timezone) {
        public static RepeatOptions defaults() {
            return new RepeatOptions(true, null);
        }
    }

    /**
     * Set unique key as string (must be index-friendly).
     */
    JobBuilder<T> uniqueKey(String uniqueKey);

    JobBuilder<T> unique(Map<String, Object> unique);

    /**
     * Set job priority.
     */
    JobBuilder<T> priority(Priority priority);

    /**
     * Set job priority raw value.
     */
    JobBuilder<T> priority(int priority);

    /**
     * Set timezone used by schedule/repeatAt/repeatEvery (cron). Null means system default.
     */
    JobBuilder<T> timezone(String timezone);

    /**
     * Schedule a one-time job to run at the specified absolute time.
     */
    JobBuilder<T> schedule(Instant time);

    /**
     * Sets a job to repeat at a specific time
     * Like cron, but only runs once per day at the specified time.
     */
    JobBuilder<T> repeatAt(String time);

    /**
     * Repeat every X amount of time.
     * Accepts human-interval strings (e.g. "5 minutes", "2 hours") or Number (seconds).
     */
    JobBuilder<T> repeatEvery(String interval);

    /**
     * Repeat by a string spec with options (skipImmediate/timezone).
     */
    JobBuilder<T> repeatEvery(String interval, RepeatOptions options);


    JobBuilder<T> repeatEvery(Number interval);

    JobBuilder<T> repeatEvery(Number interval, RepeatOptions options);

    /**
     * Mark this job as 'single'. If exists, update but do not insert a new row.
     * <p>Used by Agenda.every(...)
     */
    JobBuilder<T> single();

    /**
     * Build an immutable job spec (not persisted).
     */
    JobSpec<T> build();

    /**
     * Build + persist (upsert).
     */
    PersistResult save();

    PersistResult save(JobSpec<T> spec);
}