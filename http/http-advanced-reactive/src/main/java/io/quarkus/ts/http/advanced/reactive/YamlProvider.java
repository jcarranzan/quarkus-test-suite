package io.quarkus.ts.http.advanced.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.logging.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class YamlProvider implements MessageBodyReader<CityListDTO>, MessageBodyWriter<CityListDTO> {

  private final ObjectMapper mapper;

  public YamlProvider() {
    System.out.println("Calling YamlProvider");
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  @Override
  public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  @Override
  public CityListDTO readFrom(Class<CityListDTO> cityListDTOClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> multivaluedMap,
                              InputStream inputStream) throws IOException, WebApplicationException {
    Log.info("Parsing yaml input: " + new String(inputStream.readAllBytes()));
    return mapper.readValue(inputStream, cityListDTOClass);
  }


  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  @Override
  public void writeTo(CityListDTO cityListDTO, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
    String jsonData = mapper.writeValueAsString(cityListDTO);
    Log.info("outputStream " + jsonData);
    mapper.writeValue(outputStream, cityListDTO);
  }

}

