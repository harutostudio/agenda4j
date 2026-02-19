package io.agenda4j.core;

public enum JobType {
    SINGLE {
        @Override
        public boolean shouldReschedule() {
            return false;
        }
    },
    NORMAL {
        @Override
        public boolean shouldReschedule() {
            return true;
        }
    };

    public abstract boolean shouldReschedule();
}