package io.agenda4j;


public interface JobHandler<T> {
    String name();

    Class<T> dataClass();

    void execute(T data) throws Exception;
}