package io.quarkus.ts.http.http2;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/custom-frame")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class CustomFrameResource {
    @ConfigProperty(name = "custom.frame.response.default", defaultValue = "pong")
    String defaultCustomFrameResponse;

    @Inject
    FrameProcessingService frameProcessingService;

    @GET
    public RestResponse<String> processFrame(@QueryParam("frame") String frame) throws Exception {
        frame = Objects.requireNonNullElse(frame, defaultCustomFrameResponse);
        String processedFrame = frameProcessingService.handleCustomFrame(frame);
        System.out.println("processedFrame " + processedFrame);
        return RestResponse.ResponseBuilder.ok(processedFrame, MediaType.TEXT_PLAIN_TYPE)
                .header("X-Header", "Custom-Header")
                .build();

    }
}
