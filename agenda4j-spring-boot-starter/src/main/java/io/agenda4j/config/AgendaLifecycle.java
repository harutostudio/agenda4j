package io.agenda4j.config;

import io.agenda4j.Agenda;
import org.springframework.context.SmartLifecycle;

/**
 * Bridges Agenda start/stop lifecycle with the Spring container lifecycle.
 */
public class AgendaLifecycle implements SmartLifecycle {
    private final Agenda agenda;
    private volatile boolean running = false;

    public AgendaLifecycle(Agenda agenda) {
        this.agenda = agenda;
    }

    @Override
    public void start() {
        agenda.start();
        running = true;
    }

    @Override
    public void stop() {
        agenda.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
