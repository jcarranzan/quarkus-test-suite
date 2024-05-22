package io.quarkus.ts.http.restclient;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.GZIP;

@RegisterRestClient(configKey = "my-client")
public interface GzipClientService {
    @POST
    @Path("/gzip")
    String gzip(@GZIP byte[] message);
}
