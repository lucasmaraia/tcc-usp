package com.emailservice.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record EmailAttachmentDto(

        @NotBlank(message = "Attachment filename is required")
        String filename,

        String contentType,

        @NotBlank(message = "Attachment content is required")
        String base64Content
) implements Serializable {}
