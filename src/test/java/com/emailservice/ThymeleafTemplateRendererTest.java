package com.emailservice;

import com.emailservice.domain.exception.TemplateProcessingException;
import com.emailservice.infrastructure.service.ThymeleafTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThymeleafTemplateRendererTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
    }

    @Test
    void render_WithVariables_ReplacesVariables() {
        String template = "<html><body><h1>Hello [[${name}]]</h1><p>Your order #[[${orderId}]] is ready.</p></body></html>";
        Map<String, Object> variables = Map.of("name", "John", "orderId", "12345");

        String result = renderer.render(template, variables);

        assertEquals("<html><body><h1>Hello John</h1><p>Your order #12345 is ready.</p></body></html>", result);
    }

    @Test
    void render_WithoutVariables_ReturnsOriginal() {
        String template = "<html><body><h1>Static Content</h1></body></html>";

        String result = renderer.render(template, null);

        assertEquals(template, result);
    }

    @Test
    void render_WithNullValue_RemovesVariable() {
        String template = "<html><body><p>Hello [[${name}]]</p></body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", null);

        String result = renderer.render(template, variables);

        assertEquals("<html><body><p>Hello </p></body></html>", result);
    }

    @Test
    void render_PlainTextPlaceholders_AreLeftUntouched() {
        String template = "Hello ${name}, your code is ${code}";
        Map<String, Object> variables = Map.of("name", "Jane", "code", "ABC123");

        String result = renderer.render(template, variables);

        assertEquals("Hello ${name}, your code is ${code}", result);
    }

    @Test
    void render_WithMultipleOccurrences_ReplacesAll() {
        String template = "<div>[[${name}]]</div><span>[[${name}]]</span>";
        Map<String, Object> variables = Map.of("name", "Test");

        String result = renderer.render(template, variables);

        assertEquals("<div>Test</div><span>Test</span>", result);
    }

    @Test
    void render_WithNestedPropertyPath_IsAllowed() {
        String template = "<p>[[${customer.name}]]</p>";
        Map<String, Object> variables = Map.of("customer", Map.of("name", "Ana"));

        String result = renderer.render(template, variables);

        assertEquals("<p>Ana</p>", result);
    }

    @Test
    void render_WithMethodCallExpression_IsRejected() {
        String template = "<p>[[${T(java.lang.Runtime).getRuntime()}]]</p>";

        assertThrows(TemplateProcessingException.class, () -> renderer.render(template, Map.of()));
    }

    @Test
    void render_WithOgnlStaticAccess_IsRejected() {
        String template = "<p th:text=\"${@java.lang.Runtime@getRuntime()}\">x</p>";

        assertThrows(TemplateProcessingException.class, () -> renderer.render(template, Map.of()));
    }

    @Test
    void render_WithPreprocessingSyntax_IsRejected() {
        String template = "<p th:text=\"__${payload}__\">x</p>";

        assertThrows(TemplateProcessingException.class, () -> renderer.render(template, Map.of("payload", "1+1")));
    }
}
