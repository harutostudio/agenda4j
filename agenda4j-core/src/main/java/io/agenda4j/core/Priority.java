package io.agenda4j.core;

public enum Priority {

    HIGHEST(20),
    HIGH(10),
    NORMAL(0),
    LOW(-10),
    LOWEST(-20);

    private final int value;

    Priority(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}