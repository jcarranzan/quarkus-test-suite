package io.quarkus.ts.qe.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;

import picocli.CommandLine;

@TopCommand
@CommandLine.Command
public class OtherEntryCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        System.out.println("My name is: " + spec.name());
    }
}
