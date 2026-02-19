package io.agenda4j.core;

import io.agenda4j.JobHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobHandlerRegistry {

    private final Map<String, JobHandler<?>> handlersByName;

    public JobHandlerRegistry(List<JobHandler<?>> handlers) {
        this.handlersByName = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        JobHandler::name,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate JobHandler name: " + a.name());
                        }
                ));
    }

    public JobHandler<?> getRequired(String name) {
        JobHandler<?> handler = handlersByName.get(name);
        if (handler == null) {
            throw new IllegalStateException("No JobHandler registered for name: " + name);
        }
        return handler;
    }
}
