package io.agenda4j.utils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Date;

import org.quartz.CronExpression;

/**
 * Parses schedule specs into {@link Duration}.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>Human-readable intervals: "5 minutes", "2 hours", "1 day 3 hours"</li>
 *   <li>Cron expressions: e.g. "0 0 2 * * *" (duration is computed as time-until-next-run)</li>
 * </ul>
 * <p>
 * Note: cron is calendar-based, so the returned duration is "from -> next occurrence" and can vary.
 */
public final class IntervalParser {
    private IntervalParser() {
    }

    /**
     * Computes the next run time from persisted scheduling fields.
     *
     * @param repeatInterval    scheduled_jobs.repeatInterval
     * @param repeatTimezone    scheduled_jobs.repeatTimezone (IANA, nullable)
     * @param previousNextRunAt scheduled_jobs.nextRunAt from previous cycle
     * @param finishedAt        current execution finish time
     * @return next scheduled run time, or {@code null} for one-time jobs
     */
    public static Instant computeNextRunAt(
            String repeatInterval,
            String repeatTimezone,
            Instant previousNextRunAt,
            Instant finishedAt
    ) {

        if (repeatInterval == null || repeatInterval.isBlank()) {
            return null;
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(
                    repeatTimezone != null ? repeatTimezone : ZoneId.systemDefault().getId()
            );
        } catch (Exception e) {
            zone = ZoneId.systemDefault();
        }

        Instant baseInstant = laterOf(previousNextRunAt, finishedAt);

        /* =========================================================
         * 2. repeatAt (fixed time every day)
         * builder: repeatInterval = "AT 09:00"
         * ========================================================= */
        if (repeatInterval.startsWith("AT ")) {
            String timeOfDay = repeatInterval.substring(3).trim();
            LocalTime lt = LocalTime.parse(timeOfDay);

            ZonedDateTime base = ZonedDateTime.ofInstant(baseInstant, zone);
            ZonedDateTime candidate = base.with(lt);

            if (!candidate.isAfter(base)) {
                candidate = candidate.plusDays(1);
            }

            return candidate.toInstant();
        }

        /* =========================================================
         * 3. Cron / human-readable / numeric seconds
         * ========================================================= */
        Duration d = IntervalParser.parseDuration(
                repeatInterval,
                zone.getId(),
                baseInstant
        );

        return baseInstant.plus(d);
    }

    /* ================= helper ================= */

    private static Instant laterOf(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    /**
     * Parse a schedule spec into a {@link Duration}.
     * <p>
     * - If {@code spec} is a valid cron expression, returns the duration from {@code from} to the next occurrence.
     * - Otherwise, treats {@code spec} as a human-readable duration string.
     * <p>
     *
     * @param spec     schedule spec (cron or human-readable interval)
     * @param timezone IANA time zone id (e.g. "Asia/Taipei"); if null, system default is used
     * @param from     base instant used to compute the duration (usually {@link Instant#now()})
     */
    public static Duration parseDuration(String spec, String timezone, Instant from) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        if (from == null) {
            throw new IllegalArgumentException("from must not be null");
        }

        String s = spec.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("spec must not be empty");
        }

        if (s.matches("^\\d+$")) {
            try {
                long seconds = Long.parseLong(s);
                if (seconds <= 0) {
                    throw new IllegalArgumentException("interval seconds must be positive: " + spec);
                }
                return Duration.ofSeconds(seconds);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("interval seconds out of range: " + spec);
            }
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(timezone != null ? timezone : ZoneId.systemDefault().getId());
        } catch (Exception e) {
            zone = ZoneId.systemDefault();
        }

        try {
            String cron = normalizeCron(s);
            return parseCronDuration(cron, zone, from);
        } catch (Exception e) {
            return parseHumanDuration(s);
        }
    }

    /**
     * Convenience overload: uses system default timezone and {@link Instant#now()}.
     */
    public static Duration parseDuration(String spec) {
        return parseDuration(spec, null, Instant.now());
    }

    /**
     * Convenience overload: uses {@link Instant#now()}.
     */
    public static Duration parseDuration(String spec, String timezone) {
        return parseDuration(spec, timezone, Instant.now());
    }

    /**
     * Normalize cron expressions:
     * - Accepts 6-field Spring cron.
     * - Accepts 5-field cron by prepending seconds "0".
     */
    public static String normalizeCron(String spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        String s = spec.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("spec must not be empty");
        }

        String[] parts = s.split("\\s+");
        if (parts.length == 5) {
            return toQuartzCron("0", parts[0], parts[1], parts[2], parts[3], parts[4]);
        }
        if (parts.length == 6) {
            return toQuartzCron(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
        }
        return s;
    }

    private static String toQuartzCron(String sec, String min, String hour, String dayOfMonth, String month, String dayOfWeek) {
        String dom = dayOfMonth;
        String dow = dayOfWeek;

        if ("*".equals(dom) && "*".equals(dow)) {
            dow = "?";
        }

        return String.join(" ", sec, min, hour, dom, month, dow);
    }

    /**
     * Returns true if the string can be parsed as a Quartz {@link CronExpression}.
     */
    public static boolean looksLikeCron(String spec) {
        try {
            return CronExpression.isValidExpression(normalizeCron(spec));
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Compute duration from {@code from} to the next cron occurrence.
     */
    public static Duration parseCronDuration(String cron, ZoneId zone, Instant from) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron);
        }
        CronExpression exp;
        try {
            exp = new CronExpression(cron);
            exp.setTimeZone(java.util.TimeZone.getTimeZone(zone));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron);
        }

        Date nextDate = exp.getNextValidTimeAfter(Date.from(from));
        ZonedDateTime next = (nextDate == null)
                ? null
                : ZonedDateTime.ofInstant(nextDate.toInstant(), zone);
        if (next == null) {
            throw new IllegalArgumentException("Cron expression produced no next execution time: " + cron);
        }

        return Duration.between(from, next.toInstant());
    }

    public static Duration parseHumanDuration(String input) {
        Objects.requireNonNull(input, "input must not be null");
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Interval string must not be empty");
        }

        if (s.matches("^\\d+$")) {
            long seconds;
            try {
                seconds = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Interval seconds out of range: " + input);
            }
            if (seconds <= 0) {
                throw new IllegalArgumentException("Interval seconds must be positive: " + input);
            }
            return Duration.ofSeconds(seconds);
        }

        if (s.matches("^\\d+\\s*[smhdw]$")) {
            String digits = s.replaceAll("[^0-9]", "");
            char u = s.replaceAll("[0-9\\s]", "").charAt(0);
            long n = Long.parseLong(digits);
            return switch (u) {
                case 's' -> Duration.ofSeconds(n);
                case 'm' -> Duration.ofMinutes(n);
                case 'h' -> Duration.ofHours(n);
                case 'd' -> Duration.ofDays(n);
                case 'w' -> Duration.ofDays(7L * n);
                default -> throw new IllegalArgumentException("Unsupported compact unit: " + u);
            };
        }

        String[] parts = s.split("\\s+");
        if (parts.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid interval format. Expected pairs like '3 minutes': " + input);
        }

        boolean seenMonth = false, seenWeek = false, seenDay = false, seenHour = false, seenMinute = false, seenSecond = false;
        long totalSeconds = 0;

        for (int i = 0; i < parts.length; i += 2) {
            long n;
            try {
                n = Long.parseLong(parts[i]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number in interval: " + parts[i]);
            }
            if (n < 0) {
                throw new IllegalArgumentException("Interval values must be non-negative");
            }

            String unit = parts[i + 1];
            if (unit.endsWith("s")) {
                unit = unit.substring(0, unit.length() - 1);
            }

            switch (unit) {
                case "month" -> {
                    if (seenMonth) throw new IllegalArgumentException("Duplicate unit: month");
                    seenMonth = true;
                    totalSeconds += ChronoUnit.DAYS.getDuration().toSeconds() * 30L * n;
                }
                case "week" -> {
                    if (seenWeek) throw new IllegalArgumentException("Duplicate unit: week");
                    seenWeek = true;
                    totalSeconds += ChronoUnit.DAYS.getDuration().toSeconds() * 7L * n;
                }
                case "day" -> {
                    if (seenDay) throw new IllegalArgumentException("Duplicate unit: day");
                    seenDay = true;
                    totalSeconds += ChronoUnit.DAYS.getDuration().toSeconds() * n;
                }
                case "hour" -> {
                    if (seenHour) throw new IllegalArgumentException("Duplicate unit: hour");
                    seenHour = true;
                    totalSeconds += ChronoUnit.HOURS.getDuration().toSeconds() * n;
                }
                case "minute" -> {
                    if (seenMinute) throw new IllegalArgumentException("Duplicate unit: minute");
                    seenMinute = true;
                    totalSeconds += ChronoUnit.MINUTES.getDuration().toSeconds() * n;
                }
                case "second" -> {
                    if (seenSecond) throw new IllegalArgumentException("Duplicate unit: second");
                    seenSecond = true;
                    totalSeconds += n;
                }
                default -> throw new IllegalArgumentException("Unsupported interval unit: " + parts[i + 1]);
            }
        }

        return Duration.ofSeconds(totalSeconds);
    }
}
