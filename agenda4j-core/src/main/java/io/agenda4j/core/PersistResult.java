package io.agenda4j.core;

public record PersistResult(
        boolean created,
        boolean updated
) {
    public static PersistResult createdResult() {
        return new PersistResult(true, false);
    }

    public static PersistResult updatedResult() {
        return new PersistResult(false, true);
    }

    public static PersistResult noop() {
        return new PersistResult(false, false);
    }
}