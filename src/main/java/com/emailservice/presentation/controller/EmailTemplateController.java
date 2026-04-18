package com.emailservice.presentation.controller;

import com.emailservice.application.dto.EmailTemplateRequest;
import com.emailservice.application.dto.EmailTemplateResponse;
import com.emailservice.application.usecase.EmailTemplateUseCase;
import com.emailservice.infrastructure.service.ThymeleafService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateUseCase templateUseCase;
    private final ThymeleafService thymeleafService;

    @PostMapping
    public ResponseEntity<EmailTemplateResponse> createTemplate(
            @Valid @RequestBody EmailTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateUseCase.createTemplate(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<EmailTemplateResponse>> getTemplates(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<EmailTemplateResponse> templates = templateUseCase.getTemplatesByUser(userDetails.getUsername());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateUseCase.getTemplateById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<EmailTemplateResponse> getTemplateByName(@PathVariable String name) {
        return ResponseEntity.ok(templateUseCase.getTemplateByName(name));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<String> previewTemplate(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "{}") String variables
    ) {
        EmailTemplateResponse template = templateUseCase.getTemplateById(id);
        try {
            Map<String, Object> vars = new HashMap<>();
            if (variables != null && !variables.equals("{}")) {
                vars = new com.fasterxml.jackson.databind.ObjectMapper().readValue(variables, Map.class);
            }
            String processed = thymeleafService.processTemplate(template.getHtmlContent(), vars);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=utf-8")
                    .body(processed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing template: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<String> previewHtml(
            @RequestBody Map<String, String> request
    ) {
        try {
            String html = request.get("html");
            String varsJson = request.getOrDefault("variables", "{}");
            Map<String, Object> vars = new com.fasterxml.jackson.databind.ObjectMapper().readValue(varsJson, Map.class);
            String processed = thymeleafService.processTemplate(html, vars);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=utf-8")
                    .body(processed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody EmailTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(templateUseCase.updateTemplate(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        templateUseCase.deleteTemplate(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
