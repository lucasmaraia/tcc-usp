package com.emailservice.presentation.controller;

import com.emailservice.application.dto.EmailTemplateRequest;
import com.emailservice.application.dto.EmailTemplateResponse;
import com.emailservice.application.dto.PageResponse;
import com.emailservice.application.dto.TemplatePreviewRequest;
import com.emailservice.application.usecase.EmailTemplateUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateUseCase templateUseCase;

    @PostMapping
    public ResponseEntity<EmailTemplateResponse> createTemplate(
            @Valid @RequestBody EmailTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateUseCase.createTemplate(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmailTemplateResponse>> getTemplates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(templateUseCase.getTemplatesByUser(userDetails.getUsername(), search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> getTemplateById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(templateUseCase.getTemplateById(id, userDetails.getUsername()));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<EmailTemplateResponse> getTemplateByName(
            @PathVariable String name,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(templateUseCase.getTemplateByName(name, userDetails.getUsername()));
    }

    @PostMapping("/preview")
    public ResponseEntity<String> previewTemplate(@Valid @RequestBody TemplatePreviewRequest request) {
        String processed = templateUseCase.renderPreview(request.html(), request.variables());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(processed);
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
