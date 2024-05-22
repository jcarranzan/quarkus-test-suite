package io.quarkus.ts.http.restclient;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.GZIP;

@Path("/gzip")
public class GzipResource {

    @Inject
    @RestClient
    GzipClientService gzipClient;

    @POST
    public String gzip(@GZIP byte[] message) {
        return gzipClient.gzip(message);
    }

}
