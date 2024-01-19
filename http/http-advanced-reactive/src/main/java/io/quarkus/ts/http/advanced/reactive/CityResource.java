package io.quarkus.ts.http.advanced.reactive;

import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.APPLICATION_YAML;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.logging.Log;

@Path("/city")
public class CityResource {

    private static final String YAML_FILE_PATH = "payload.yaml";

    private static final Logger LOG = Logger.getLogger(CityResource.class);

    private final ObjectMapper objectMapper = new YAMLMapper();

    @Inject
    private CityListWrapperSerializer serializer;

    private List<City> cityList = new ArrayList<>();

    @GET
    @Produces(value = MediaType.APPLICATION_XML)
    public CityListWrapper getCities() {
        LOG.info("Received request to getCities");

        if (cityList.isEmpty()) {
            cityList.add(new City("San Bernardino", "EEUU"));
            cityList.add(new City("Brno", "Czech Republic"));
            cityList.add(new City("Zaragoza", "Spain"));
        }
        return new CityListWrapper(cityList);
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(value = MediaType.APPLICATION_XML)
    public String createCity(CityListDTO cityListDTO) {

        if (cityListDTO == null) {
            LOG.error("Error deserializing XML");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Invalid XML payload</error>")
                    .build().toString();
        }

        String responseXML = serializer.toXML(cityListDTO);

        return responseXML;
    }

    @POST
    @Path("/cities")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/x-yaml")
    public Object handleConsumedYamlPostRequest(@RequestBody final String yamlPayload) throws IOException {

        LOG.info("Received YAML payload: " + yamlPayload);

        try {
            return objectMapper.readValue(yamlPayload, Object.class);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error parsing YAML: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/getYamlFile")
    @Produces(APPLICATION_YAML)
    public Response getYamlFile() throws IOException {
        try {
            CityListDTO cityListDTO = readYamlFile(YAML_FILE_PATH);

            if (cityListDTO != null) {
                LOG.info("content ----! " + cityListDTO);
                return Response.ok(cityListDTO).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error reading YAML file")
                        .type("text/plain")
                        .build();
            }
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error reading YAML file: " + e.getMessage())
                    .type("text/plain")
                    .build();
        }
    }

    private CityListDTO readYamlFile(String yamlFilePath) throws IOException {
        java.nio.file.Path path = Paths.get(yamlFilePath);
        if (!Files.exists(path)) {
            throw new IOException("YAML file not found: " + yamlFilePath);
        }

        try {
            byte[] yamlData = Files.readAllBytes(path);
            Yaml yamlParser = new Yaml();
            List<City> cities = yamlParser.loadAs(new ByteArrayInputStream(yamlData), List.class);
            return new CityListDTO(cities);
        } catch (IOException e) {
            LOG.error("Error reading YAML file: {}", yamlFilePath, e);
            return null;
        }
    }

    @GET
    @Path("/{imageName}")
    @Consumes("image/jpg")
    public Response getImage(@PathParam("imageName") String imageName) {
        try {
            java.nio.file.Path imagePath = Paths.get("wolf_moon_howling.jpg");
            if (Files.exists(imagePath)) {
                byte[] imageData = Files.readAllBytes(imagePath);
                Log.info("Image retrieval successful for {} " + imageName);
                return Response.ok(imageData, determineContentType(imageName)).build();
            } else {
                Log.info("Image not found: {} " + imageName);
                return Response.status(Response.Status.NOT_FOUND).entity("Image not found").build();
            }
        } catch (IOException e) {
            Log.error("Error reading image for {}", imageName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error reading image").build();
        }
    }

    @POST
    @Path("/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] postFormReturnFile(@RestForm("image") @PartType("image/jpg") File image) throws IOException {
        return IOUtils.toByteArray(image.toURI());
    }

    private String determineContentType(String imageName) {
        if (imageName.endsWith(".png")) {
            return "image/png";
        } else if (imageName.endsWith(".jpg") || imageName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }

}
