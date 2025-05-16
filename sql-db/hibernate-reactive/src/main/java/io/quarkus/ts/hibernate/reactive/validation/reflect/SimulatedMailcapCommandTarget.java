package io.quarkus.ts.hibernate.reactive.validation.reflect;

public class SimulatedMailcapCommandTarget {
    public SimulatedMailcapCommandTarget() {
        System.out.println("**** SimulatedMailcapCommandTarget instantiated");
    }

    public void executeCommand() {
        System.out.println("**** SimulatedMailcapCommandTarget.executeCommand() called");
    }
}