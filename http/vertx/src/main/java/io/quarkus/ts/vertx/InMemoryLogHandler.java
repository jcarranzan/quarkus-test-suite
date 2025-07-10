package io.quarkus.ts.vertx;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtHandler;

public class InMemoryLogHandler extends ExtHandler {

    private static final List<String> recordList = new CopyOnWriteArrayList<>();

    public static List<String> getRecords() {
        return Collections.unmodifiableList(recordList);
    }

    public static void reset() {
        recordList.clear();
    }

    @Override
    public void publish(LogRecord record) {
        if (getFormatter() != null) {
            String formatted = getFormatter().format(record);
            if (formatted != null && !formatted.isEmpty()) {
                recordList.add(formatted);
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
