package io.quarkus.ts.lifecycle;

import java.util.Set;

import jakarta.ws.rs.core.Application;

public abstract class MyApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(ArgsResource.class);
    }
}
