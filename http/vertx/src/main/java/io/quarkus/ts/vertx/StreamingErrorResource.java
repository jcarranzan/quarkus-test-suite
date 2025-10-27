package io.quarkus.ts.vertx;

import java.util.stream.IntStream;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.impl.NoStackTraceException;

@Path("/streaming-error")
public class StreamingErrorResource {
    @GET
    public Multi<String> stream(@RestQuery @DefaultValue("false") boolean fail) {
        return Multi.createFrom().emitter(emitter -> {
            emit(emitter);
            if (fail) {
                throw new NoStackTraceException("dummy failure");
            } else {
                emit(emitter);
                emitter.complete();
            }
        });
    }

    private static void emit(MultiEmitter<? super String> emitter) {
        IntStream.range(0, 100).forEach(i -> {
            emitter.emit(String.valueOf(i));
        });
    }

}
