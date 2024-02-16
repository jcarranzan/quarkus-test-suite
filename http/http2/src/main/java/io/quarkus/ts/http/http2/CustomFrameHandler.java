package io.quarkus.ts.http.http2;

public interface CustomFrameHandler {
    String handleCustomFrame(String frame) throws Exception;
}
