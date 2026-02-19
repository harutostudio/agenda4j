package io.agenda4j.core;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable job definition produced by JobBuilder.build().
 * This is a pure data object with no persistence logic.
 */
public record JobSpec<T>(

        // identity
        String name,
        String uniqueKey,
        Map<String, Object> unique,
        JobType type,

        // scheduling
        Instant nextRunAt,
        String repeatInterval,
        String repeatTimezone,

        // execution metadata
        int priority,

        // payload
        T data
) {
}