package io.quarkus.ts.http.http2;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FrameProcessingService implements CustomFrameHandler {
    @Override
    public String handleCustomFrame(String frame) {
        return "Processed frame: " + frame;
    }
}
