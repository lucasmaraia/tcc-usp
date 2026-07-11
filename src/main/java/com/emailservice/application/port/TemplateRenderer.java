package com.emailservice.application.port;

import java.util.Map;

public interface TemplateRenderer {
    String render(String templateContent, Map<String, Object> variables);
}
