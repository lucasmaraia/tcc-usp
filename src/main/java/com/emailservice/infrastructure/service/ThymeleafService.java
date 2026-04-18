package com.emailservice.infrastructure.service;

import com.emailservice.domain.exception.TemplateProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class ThymeleafService {

    private final TemplateEngine templateEngine;

    public ThymeleafService() {
        this.templateEngine = createTemplateEngine();
    }

    private TemplateEngine createTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        
        engine.setTemplateResolver(resolver);
        
        return engine;
    }

    public String processTemplate(String templateContent, Map<String, Object> variables) {
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            return templateEngine.process(templateContent, context);
        } catch (Exception e) {
            log.error("Failed to process Thymeleaf template: {}", e.getMessage(), e);
            throw new TemplateProcessingException("Failed to process email template: " + e.getMessage(), e);
        }
    }
}
