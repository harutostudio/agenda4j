package io.agenda4j.internal;

import io.agenda4j.JobBuilder;
import io.agenda4j.core.*;
import io.agenda4j.utils.IntervalParser;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Default {@link JobBuilder} implementation used by the Mongo-backed scheduler.
 */
public class SimpleJobBuilder<T> implements JobBuilder<T> {

    private final String name;
    private final T data;
    private final Function<JobSpec<T>, PersistResult> persister;

    private String uniqueKey;
    private Map<String, Object> unique;
    private JobType type = JobType.NORMAL;

    private Instant nextRunAt;
    private String repeatInterval;
    private String repeatTimezone;

    private int priority = Priority.NORMAL.value();

    public SimpleJobBuilder(String name, T data, Function<JobSpec<T>, PersistResult> persister) {
        this.name = Objects.requireNonNull(name, "job name must not be null");
        this.data = data;
        this.persister = Objects.requireNonNull(persister, "persister must not be null");
    }

    @Override
    public JobBuilder<T> uniqueKey(String uniqueKey) {
        Objects.requireNonNull(uniqueKey, "uniqueKey must not be null");
        if (uniqueKey.isBlank()) throw new IllegalArgumentException("uniqueKey must not be blank");

        this.uniqueKey = uniqueKey;
        this.type = JobType.NORMAL;
        return this;
    }

    @Override
    public JobBuilder<T> unique(Map<String, Object> unique) {
        Objects.requireNonNull(unique, "unique must not be null");
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("unique must not be empty");
        }

        for (var e : unique.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k == null || k.isBlank()) {
                throw new IllegalArgumentException("unique contains blank key");
            }
            if (v == null) {
                throw new IllegalArgumentException("unique contains null value for key: " + k);
            }
        }

        this.unique = Map.copyOf(unique);
        return this;
    }

    @Override
    public JobBuilder<T> priority(Priority priority) {
        Objects.requireNonNull(priority, "priority must not be null");
        this.priority = priority.value();
        return this;
    }

    @Override
    public JobBuilder<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public JobBuilder<T> timezone(String timezone) {
        Objects.requireNonNull(timezone, "timezone must not be null");
        ZoneId.of(timezone);
        this.repeatTimezone = timezone;
        return this;
    }

    @Override
    public JobBuilder<T> schedule(Instant time) {
        Objects.requireNonNull(time, "time must not be null");
        this.nextRunAt = time;
        return this;
    }

    @Override
    public JobBuilder<T> repeatAt(String timeOfDay) {
        Objects.requireNonNull(timeOfDay, "timeOfDay must not be null");
        LocalTime lt = parseTimeOfDay(timeOfDay);

        String tz = this.repeatTimezone != null ? this.repeatTimezone : ZoneId.systemDefault().getId();
        ZoneId zone = ZoneId.of(tz);

        this.repeatInterval = "AT " + timeOfDay;
        this.repeatTimezone = tz;
        if (this.nextRunAt != null) {
            return this;
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime candidate = now.with(lt);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        this.nextRunAt = candidate.toInstant();

        return this;
    }

    @Override
    public JobBuilder<T> repeatEvery(String intervalOrCron) {
        return repeatEvery(intervalOrCron, RepeatOptions.defaults());
    }

    @Override
    public JobBuilder<T> repeatEvery(String intervalOrCron, RepeatOptions options) {
        Objects.requireNonNull(intervalOrCron, "intervalOrCron must not be null");

        if (options == null) {
            options = RepeatOptions.defaults();
        }

        if (options.timezone() != null) {
            timezone(options.timezone());
        }

        this.repeatInterval = intervalOrCron;
        if (this.nextRunAt != null) {
            return this;
        }

        if (!options.skipImmediate()) {
            this.nextRunAt = Instant.now();
            return this;
        }

        String tz = this.repeatTimezone != null ? this.repeatTimezone : ZoneId.systemDefault().getId();

        Duration d = IntervalParser.parseDuration(intervalOrCron, tz);
        this.nextRunAt = Instant.now().plus(d);
        return this;
    }

    @Override
    public JobBuilder<T> repeatEvery(Number interval) {
        return repeatEvery(interval, RepeatOptions.defaults());
    }

    @Override
    public JobBuilder<T> repeatEvery(Number interval, RepeatOptions options) {
        Objects.requireNonNull(interval, "interval must not be null");

        double asDouble = interval.doubleValue();
        if (asDouble <= 0) {
            throw new IllegalArgumentException("interval must be a positive number of seconds");
        }
        if (asDouble % 1 != 0) {
            throw new IllegalArgumentException("interval must be an integer number of seconds");
        }

        long seconds = interval.longValue();

        if (options == null) {
            options = RepeatOptions.defaults();
        }
        if (options.timezone() != null) {
            timezone(options.timezone());
        }

        this.repeatInterval = interval.toString();
        if (this.nextRunAt != null) {
            return this;
        }

        Instant now = Instant.now();
        if (!options.skipImmediate()) {
            this.nextRunAt = now;
        } else {
            this.nextRunAt = now.plusSeconds(seconds);
        }
        return this;
    }

    @Override
    public JobBuilder<T> single() {
        this.uniqueKey = null;
        this.unique= null;
        this.type = JobType.SINGLE;
        return this;
    }

    @Override
    public JobSpec<T> build() {
        return new JobSpec<>(
                name,
                uniqueKey,
                unique,
                type,
                nextRunAt,
                repeatInterval,
                repeatTimezone,
                priority,
                data
        );
    }

    @Override
    public PersistResult save() {
        JobSpec<T> spec = this.build();
        return persister.apply(spec);
    }

    @Override
    public PersistResult save(JobSpec<T> spec) {
        return persister.apply(spec);
    }

    private LocalTime parseTimeOfDay(String timeOfDay) {
        try {
            return LocalTime.parse(timeOfDay);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid timeOfDay. Expected HH:mm or HH:mm:ss: " + timeOfDay);
        }
    }
}
