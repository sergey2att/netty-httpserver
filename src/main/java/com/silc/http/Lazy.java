package com.silc.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<T> {
    private static final Logger log = LoggerFactory.getLogger(Lazy.class);

    private final Supplier<T> initializer;
    private boolean initialized;
    protected T value;


    public Lazy(@Nonnull Supplier<T> initializer) {
        this.initializer = Objects.requireNonNull(initializer, "initializer cannot be null");
    }

    public T getValue() {
        if (!initialized) {
            log.trace("Initializing {}", this);
            value = initializer.get();
            initialized = true;
            log.trace("Initialized {}", this);
        }

        return value;
    }
}
