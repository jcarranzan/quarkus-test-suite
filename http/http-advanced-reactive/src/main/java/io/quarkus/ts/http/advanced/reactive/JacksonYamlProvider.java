package io.quarkus.ts.http.advanced.reactive;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.jakarta.rs.base.ProviderBase;
import com.fasterxml.jackson.jakarta.rs.cfg.Annotations;
import com.fasterxml.jackson.jakarta.rs.yaml.PackageVersion;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLEndpointConfig;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMapperConfigurator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;

@Provider
@Consumes(MediaType.WILDCARD) // NOTE: required to support "non-standard" JSON variants
@Produces(MediaType.WILDCARD)
public class JacksonYamlProvider
  extends ProviderBase<JacksonYamlProvider,
    YAMLMapper,
    YAMLEndpointConfig,
    YAMLMapperConfigurator> {
  /**
   * Default annotation sets to use, if not explicitly defined during
   * construction: only Jackson annotations are used for the base
   * class. Sub-classes can use other settings.
   */
  public final static Annotations[] BASIC_ANNOTATIONS = {
    Annotations.JACKSON
  };

    /*
    /**********************************************************
    /* Context configuration
    /**********************************************************
     */

  /**
   * Injectable context object used to locate configured
   * instance of {@link YAMLMapper} to use for actual
   * serialization.
   */
  @Context
  protected Providers _providers;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

  /**
   * Default constructor, usually used when provider is automatically
   * configured to be used with JAX-RS implementation.
   */
  public JacksonYamlProvider() {
    this(null, BASIC_ANNOTATIONS);
  }

  /**
   * @param annotationsToUse Annotation set(s) to use for configuring
   *                         data binding
   */
  public JacksonYamlProvider(Annotations... annotationsToUse) {
    this(null, annotationsToUse);
  }

  public JacksonYamlProvider(YAMLMapper mapper) {
    this(mapper, BASIC_ANNOTATIONS);
  }

  /**
   * Constructor to use when a custom mapper (usually components
   * like serializer/deserializer factories that have been configured)
   * is to be used.
   *
   * @param annotationsToUse Sets of annotations (Jackson, JAXB) that provider should
   *                         support
   */
  public JacksonYamlProvider(YAMLMapper mapper, Annotations[] annotationsToUse) {
    super(new YAMLMapperConfigurator(mapper, annotationsToUse));
  }

  /**
   * Method that will return version information stored in and read from jar
   * that contains this class.
   */
  @Override
  public Version version() {
    return PackageVersion.VERSION;
  }

    /*
    /**********************************************************
    /* Abstract method impls
    /**********************************************************
     */

  @Override
  protected YAMLEndpointConfig _configForReading(ObjectReader reader,
                                                 Annotation[] annotations) {
    return YAMLEndpointConfig.forReading(reader, annotations);
  }

  @Override
  protected YAMLEndpointConfig _configForWriting(ObjectWriter writer,
                                                 Annotation[] annotations) {
    return YAMLEndpointConfig.forWriting(writer, annotations);
  }

  /**
   * Helper method used to check whether given media type
   * is YAML type or sub type.
   * Current implementation essentially checks to see whether
   * {@link MediaType#getSubtype} returns "xml" or something
   * ending with "+yaml".
   */
  @Override
  protected boolean hasMatchingMediaType(MediaType mediaType) {
    /* As suggested by Stephen D, there are 2 ways to check: either
     * being as inclusive as possible (if subtype is "yaml"), or
     * exclusive (major type "application", minor type "yaml").
     * Let's start with inclusive one, hard to know which major
     * types we should cover aside from "application".
     */
    if (mediaType != null) {
      boolean matching = mediaType.isWildcardType();
      System.out.println("Matching media type: " + matching);
      String subtype = mediaType.getSubtype();
      return "yaml".equalsIgnoreCase(subtype) || subtype.endsWith("+yaml");
      //tarik: apparently there is not a standard for yaml types, should be discussed
    }
    /* Not sure if this can happen; but it seems reasonable
     * that we can at least produce yaml without media type?
     */
    return true;
  }

  /**
   * Method called to locate {@link YAMLMapper} to use for serialization
   * and deserialization. If an instance has been explicitly defined by
   * {@link #setMapper} (or non-null instance passed in constructor), that
   * will be used.
   * If not, will try to locate it using standard JAX-RS
   * {@link ContextResolver} mechanism, if it has been properly configured
   * to access it (by JAX-RS runtime).
   * Finally, if no mapper is found, will return a default unconfigured
   * {@link ObjectMapper} instance (one constructed with default constructor
   * and not modified in any way)
   *
   * @param type      Class of object being serialized or deserialized;
   *                  not checked at this point, since it is assumed that unprocessable
   *                  classes have been already weeded out,
   *                  but will be passed to {@link ContextResolver} as is.
   * @param mediaType Declared media type for the instance to process:
   *                  not used by this method,
   *                  but will be passed to {@link ContextResolver} as is.
   */
  @Override
  public YAMLMapper _locateMapperViaProvider(Class<?> type, MediaType mediaType) {
    // First: were we configured with a specific instance?
    YAMLMapper m = _mapperConfig.getConfiguredMapper();
    if (m == null) {
      // If not, maybe we can get one configured via context?
      if (_providers != null) {
        ContextResolver<YAMLMapper> resolver = _providers.getContextResolver(YAMLMapper.class, mediaType);
        /* Above should work as is, but due to this bug
         *   [https://jersey.dev.java.net/issues/show_bug.cgi?id=288]
         * in Jersey, it doesn't. But this works until resolution of
         * the issue:
         */
        if (resolver == null) {
          resolver = _providers.getContextResolver(YAMLMapper.class, null);
        }
        if (resolver != null) {
          m = resolver.getContext(type);
        }
      }
      if (m == null) {
        // If not, let's get the fallback default instance
        m = _mapperConfig.getDefaultMapper();
      }
    }
    return m;
  }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

  @Override
  protected JsonParser _createParser(ObjectReader reader, InputStream rawStream)
    throws IOException {
    // Fix for [Issue#4]: note, can not try to advance parser, XML parser complains
    PushbackInputStream wrappedStream = new PushbackInputStream(rawStream);
    int firstByte = wrappedStream.read();
    if (firstByte == -1) {
      return null;
    }
    wrappedStream.unread(firstByte);
    return reader.getFactory().createParser(wrappedStream);
  }
}
