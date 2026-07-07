package com.emailservice.application.port;

import java.util.Map;

/**
 * Port for rendering an email template with a set of variables,
 * implemented by the infrastructure layer.
 */
public interface TemplateRenderer {
    String render(String templateContent, Map<String, Object> variables);
}
