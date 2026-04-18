package com.emailservice;

import com.emailservice.infrastructure.service.ThymeleafService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThymeleafServiceTest {

    private ThymeleafService thymeleafService;

    @BeforeEach
    void setUp() {
        thymeleafService = new ThymeleafService();
    }

    @Test
    void processTemplate_WithVariables_ReplacesVariables() {
        String template = "<html><body><h1>Hello [[${name}]]</h1><p>Your order #[[${orderId}]] is ready.</p></body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        variables.put("orderId", "12345");

        String result = thymeleafService.processTemplate(template, variables);

        assertEquals("<html><body><h1>Hello John</h1><p>Your order #12345 is ready.</p></body></html>", result);
    }

    @Test
    void processTemplate_WithoutVariables_ReturnsOriginal() {
        String template = "<html><body><h1>Static Content</h1></body></html>";

        String result = thymeleafService.processTemplate(template, null);

        assertEquals(template, result);
    }

    @Test
    void processTemplate_WithNullValue_RemovesVariable() {
        String template = "<html><body><p>Hello [[${name}]]</p></body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", null);

        String result = thymeleafService.processTemplate(template, variables);

        assertEquals("<html><body><p>Hello </p></body></html>", result);
    }

    @Test
    void processTextTemplate_WithVariables_ReplacesVariables() {
        String template = "Hello ${name}, your code is ${code}";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Jane");
        variables.put("code", "ABC123");

        String result = thymeleafService.processTextTemplate(template, variables);

        assertEquals("Hello ${name}, your code is ${code}", result);
    }

    @Test
    void processTemplate_WithMultipleOccurrences_ReplacesAll() {
        String template = "<div>[[${name}]]</div><span>[[${name}]]</span>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test");

        String result = thymeleafService.processTemplate(template, variables);

        assertEquals("<div>Test</div><span>Test</span>", result);
    }
}
