package com.emailservice.application.port;

import com.emailservice.application.dto.EmailMessageDto;

public interface EmailQueueGateway {
    void sendEmail(EmailMessageDto emailMessage);
    void sendToRetry(EmailMessageDto emailMessage);
    void sendToDlq(EmailMessageDto emailMessage);
}
