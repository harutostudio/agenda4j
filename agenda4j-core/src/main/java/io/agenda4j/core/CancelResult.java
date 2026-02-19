package io.agenda4j.core;

/**
 * Result of canceling jobs.
 *
 * matched  : number of jobs matched by the query
 * modified : number of jobs modified (e.g. nextRunAt cleared)
 * deleted  : number of jobs deleted
 */
public record CancelResult(
        long matched,
        long modified,
        long deleted
) {

    public static CancelResult empty() {
        return new CancelResult(0, 0, 0);
    }

    public boolean hasEffect() {
        return modified > 0 || deleted > 0;
    }
}