package io.agenda4j.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntervalParserTest {

    @Test
    void parseHumanDurationShouldWork() {
        Duration duration = IntervalParser.parseDuration("5 minutes", "UTC", Instant.parse("2026-01-01T00:00:00Z"));
        assertEquals(Duration.ofMinutes(5), duration);
    }

    @Test
    void parseCronDurationShouldSupportFiveFieldCron() {
        Duration duration = IntervalParser.parseDuration("*/5 * * * *", "UTC", Instant.parse("2026-01-01T00:01:00Z"));
        assertEquals(Duration.ofMinutes(4), duration);
    }

    @Test
    void computeNextRunAtShouldUseLaterOfPreviousAndFinished() {
        Instant previousNextRunAt = Instant.parse("2026-01-01T00:05:00Z");
        Instant finishedAt = Instant.parse("2026-01-01T00:06:00Z");

        Instant next = IntervalParser.computeNextRunAt(
                "*/5 * * * *",
                "UTC",
                previousNextRunAt,
                finishedAt
        );

        assertEquals(Instant.parse("2026-01-01T00:10:00Z"), next);
    }

    @Test
    void computeNextRunAtShouldSupportAtSyntax() {
        Instant previousNextRunAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant finishedAt = Instant.parse("2026-01-01T10:01:00Z");

        Instant next = IntervalParser.computeNextRunAt("AT 10:00", "UTC", previousNextRunAt, finishedAt);

        assertEquals(Instant.parse("2026-01-02T10:00:00Z"), next);
    }

    @Test
    void looksLikeCronShouldRecognizeValidSpec() {
        assertTrue(IntervalParser.looksLikeCron("0 */10 * * * *"));
    }
}
