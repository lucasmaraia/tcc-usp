package com.emailservice.infrastructure.messaging;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.port.EmailQueueGateway;
import com.emailservice.application.port.EmailSender;
import com.emailservice.application.port.TemplateRenderer;
import com.emailservice.application.usecase.SendEmailUseCase;
import com.emailservice.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private static final int MAX_RETRIES = 3;

    private final TemplateRenderer templateRenderer;
    private final EmailSender emailSender;
    private final EmailQueueGateway emailQueueGateway;
    private final SendEmailUseCase sendEmailUseCase;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailMessageDto emailMessage) {
        log.info("Received email {} for recipient {}", emailMessage.id(), emailMessage.toEmail());

        if (sendEmailUseCase.isAlreadySent(emailMessage.id())) {
            log.warn("Email {} already sent, skipping", emailMessage.id());
            return;
        }

        try {
            String htmlBody = templateRenderer.render(emailMessage.htmlContent(), emailMessage.variables());
            emailSender.sendEmail(emailMessage.toEmail(), emailMessage.subject(), htmlBody,
                    emailMessage.attachments());
            sendEmailUseCase.markSent(emailMessage.id(), htmlBody);
            log.info("Email {} sent to {}", emailMessage.id(), emailMessage.toEmail());
        } catch (Exception e) {
            log.error("Failed to process email {} for recipient {}",
                    emailMessage.id(), emailMessage.toEmail(), e);
            handleFailure(emailMessage, e);
        }
    }

    private void handleFailure(EmailMessageDto emailMessage, Exception e) {
        if (emailMessage.retryCount() < MAX_RETRIES) {
            int nextAttempt = emailMessage.retryCount() + 1;
            log.info("Scheduling retry {} for email {}", nextAttempt, emailMessage.id());
            sendEmailUseCase.markRetrying(emailMessage.id());
            emailQueueGateway.sendToRetry(emailMessage.withRetryCount(nextAttempt));
        } else {
            log.error("Max retries exceeded for email {}. Sending to DLQ.", emailMessage.id());
            sendEmailUseCase.markFailed(emailMessage.id(), e.getMessage());
            emailQueueGateway.sendToDlq(emailMessage);
        }
    }
}
