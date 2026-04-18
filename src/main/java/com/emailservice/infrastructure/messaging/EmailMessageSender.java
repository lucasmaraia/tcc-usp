package com.emailservice.infrastructure.messaging;

import com.emailservice.application.dto.EmailMessageDto;

public interface EmailMessageSender {
    void sendEmail(EmailMessageDto emailMessage);
    void sendToRetry(EmailMessageDto emailMessage);
    void sendToDlq(EmailMessageDto emailMessage);
}
