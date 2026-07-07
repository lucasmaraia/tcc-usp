package com.emailservice.application.port;

import com.emailservice.application.dto.EmailMessageDto;

/**
 * Port for publishing email messages to the message broker,
 * implemented by the infrastructure layer.
 */
public interface EmailQueueGateway {
    void sendEmail(EmailMessageDto emailMessage);
    void sendToRetry(EmailMessageDto emailMessage);
    void sendToDlq(EmailMessageDto emailMessage);
}
