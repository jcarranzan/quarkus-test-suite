package io.quarkus.ts.hibernate.reactive.validation.feature;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

public class HibernateValidationFeatureReproducer implements Feature {

    private static final String SIMULATED_MAILCAP_RESOURCE = "META-INF/my-simulated-mailcap.txt";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Set<String> commandClassNames = new HashSet<>();

        // 1. Simulate finding and parsing a resource like mailcap
        try {
            Enumeration<URL> urls = access.getApplicationClassLoader().getResources(SIMULATED_MAILCAP_RESOURCE);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream is = url.openStream();
                        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("command=")) {
                            commandClassNames.add(line.substring("command=".length()).trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("HibernateValidationFeatureReproducer: Error processing resource " + SIMULATED_MAILCAP_RESOURCE
                    + ": " + e.getMessage());
            // For a test, we might not want to throw a hard error here if the resource isn't found,
            // but for real features this would be important.
        }

        // 2. Register discovered "command" classes for reflection
        for (String className : commandClassNames) {
            registerClassForReflection(access, className);
        }

        // 3. Additionally, register some common JDK classes for reflection,
        // as Hibernate Validator might encounter these during deep validation of collections.
        // This was part of our "AdvancedFeatureReproducer" idea.
        registerClassForReflection(access, "java.util.ArrayList");
        registerClassForReflection(access, "java.util.HashMap");
    }

    private void registerClassForReflection(BeforeAnalysisAccess access, String className) {
        Class<?> clazz = access.findClassByName(className);
        if (clazz != null) {
            try {
                RuntimeReflection.register(clazz);
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    RuntimeReflection.register(ctor);
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    RuntimeReflection.register(method);
                }
                System.out.println("HibernateValidationFeatureReproducer: Registered " + className + " for reflection.");
            } catch (Exception e) {
                System.err.println("HibernateValidationFeatureReproducer: Failed to register " + className + " for reflection: "
                        + e.getMessage());
            }
        } else {
            System.err.println("HibernateValidationFeatureReproducer: Class not found for reflection: " + className);
        }
    }

    @Override
    public String getDescription() {
        return "Feature mimicking AngusActivationFeature's reflection registration from resources, for #47033.";
    }

    // We can keep this always enabled for the test.
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return true;
    }

}
