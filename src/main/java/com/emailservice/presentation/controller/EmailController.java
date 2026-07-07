package com.emailservice.presentation.controller;

import com.emailservice.application.dto.EmailMessageResponse;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.application.usecase.SendEmailUseCase;
import com.emailservice.domain.entity.EmailMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final SendEmailUseCase sendEmailUseCase;

    @PostMapping("/send")
    public ResponseEntity<EmailMessageResponse> sendEmail(
            @Valid @RequestBody SendEmailRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(sendEmailUseCase.sendEmail(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<EmailMessageResponse>> getMessages(
            @RequestParam(required = false) EmailMessage.EmailStatus status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sendEmailUseCase.getMessages(userDetails.getUsername(), status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailMessageResponse> getMessageById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sendEmailUseCase.getMessageById(id, userDetails.getUsername()));
    }
}
