package io.quarkus.ts.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class InMemoryLogHandler extends Handler {
    private static final List<String> LOG_RECORDS = Collections.synchronizedList(new ArrayList<>());

    public static void reset() {
        LOG_RECORDS.clear();
    }

    public static List<String> getRecords() {
        return new ArrayList<>(LOG_RECORDS);
    }

    @Override
    public void publish(LogRecord logRecord) {
        String formattedMessage = getFormatter().format(logRecord);
        LOG_RECORDS.add(formattedMessage);
        System.out.println("InMemoryLogHandler captured: " + formattedMessage);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
