package com.emailservice.infrastructure.service;

import com.emailservice.application.port.TemplateRenderer;
import com.emailservice.domain.exception.TemplateProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders user-provided templates with Thymeleaf.
 *
 * <p>Because template content is user input, full expression evaluation would allow
 * server-side template injection (remote code execution via SpEL/OGNL). Before rendering,
 * every expression in the template is validated against a whitelist that only allows
 * simple variable references such as {@code ${name}} or {@code ${customer.address}}.</p>
 */
@Service
@Slf4j
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    /** Matches ${...}, *{...}, #{...}, @{...} and ~{...} expressions. */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("[$*#@~]\\{([^}]*)}");

    /** Only bare variable names with optional dotted property paths are allowed. */
    private static final Pattern SAFE_EXPRESSION_CONTENT =
            Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*");

    private final SpringTemplateEngine templateEngine;

    public ThymeleafTemplateRenderer() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        this.templateEngine = new SpringTemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    @Override
    public String render(String templateContent, Map<String, Object> variables) {
        validateTemplate(templateContent);
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            if (variables != null) {
                context.setVariables(variables);
            }
            return templateEngine.process(templateContent, context);
        } catch (Exception e) {
            log.error("Failed to process template", e);
            throw new TemplateProcessingException("Failed to process email template: " + e.getMessage(), e);
        }
    }

    private void validateTemplate(String templateContent) {
        if (templateContent == null) {
            throw new TemplateProcessingException("Template content must not be null");
        }

        // "__" enables Thymeleaf expression preprocessing, which would evaluate
        // variable values as expressions; reject it outright.
        if (templateContent.contains("__$")) {
            throw new TemplateProcessingException("Template contains disallowed preprocessing syntax");
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String expression = matcher.group();
            String content = matcher.group(1).trim();
            boolean isVariableExpression = expression.startsWith("$");

            if (!isVariableExpression || !SAFE_EXPRESSION_CONTENT.matcher(content).matches()) {
                throw new TemplateProcessingException(
                        "Template contains a disallowed expression: " + expression
                                + ". Only simple variables like ${name} are supported.");
            }
        }
    }
}
