package com.emailservice.presentation.controller;

import com.emailservice.application.dto.EmailMessageResponse;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.application.usecase.SendEmailUseCase;
import com.emailservice.domain.entity.EmailMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(sendEmailUseCase.sendEmail(request, token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailMessageResponse> getMessageById(@PathVariable UUID id) {
        return ResponseEntity.ok(sendEmailUseCase.getMessageById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmailMessageResponse>> getMessagesByStatus(@PathVariable String status) {
        EmailMessage.EmailStatus emailStatus = EmailMessage.EmailStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(sendEmailUseCase.getMessagesByStatus(emailStatus));
    }

    @GetMapping("/to/{email}")
    public ResponseEntity<List<EmailMessageResponse>> getMessagesByEmail(@PathVariable String email) {
        return ResponseEntity.ok(sendEmailUseCase.getMessagesByEmail(email));
    }
}
