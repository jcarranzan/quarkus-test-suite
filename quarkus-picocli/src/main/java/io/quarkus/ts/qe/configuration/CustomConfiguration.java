package io.quarkus.ts.qe.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.logging.Log;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;

import picocli.CommandLine;

@ApplicationScoped
public class CustomConfiguration {

    @Produces
    public CommandLine customCommandLine(PicocliCommandLineFactory commandLineFactory) {
        Log.info("CustomConfiguration..........");
        return commandLineFactory.create().setCommandName("customized-config");
    }
}
