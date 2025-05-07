package io.quarkus.ts.qute;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
public record UserWithOrder(String name, int id, Long orderNumber, boolean isActive) implements TemplateInstance {
}
