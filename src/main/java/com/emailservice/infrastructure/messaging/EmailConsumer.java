package com.emailservice.infrastructure.messaging;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.infrastructure.config.RabbitMQConfig;
import com.emailservice.infrastructure.service.EmailSenderService;
import com.emailservice.infrastructure.service.ThymeleafService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final ThymeleafService thymeleafService;
    private final EmailSenderService emailSenderService;
    private final EmailMessageSender emailMessageSender;
    private final EmailMessageRepository messageRepository;

    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailMessageDto emailMessage) {
        log.info("Received email message: {} for recipient: {}",
                emailMessage.getId(), emailMessage.getToEmail());

        if (messageRepository.existsByIdAndStatus(emailMessage.getId(), EmailMessage.EmailStatus.SENT)) {
            log.warn("Email {} already sent, skipping", emailMessage.getId());
            return;
        }

        try {
            String htmlBody = thymeleafService.processTemplate(
                    emailMessage.getHtmlContent(),
                    emailMessage.getVariables()
            );

            emailSenderService.sendEmail(
                    emailMessage.getToEmail(),
                    emailMessage.getSubject(),
                    htmlBody,
                    null
            );

            messageRepository.findById(emailMessage.getId()).ifPresent(msg -> {
                msg.setStatus(EmailMessage.EmailStatus.SENT);
                msg.setSentAt(java.time.LocalDateTime.now());
                messageRepository.save(msg);
            });

            log.info("Email sent successfully: {} to {}", emailMessage.getId(), emailMessage.getToEmail());

        } catch (Exception e) {
            log.error("Failed to process email: {} for recipient: {}",
                    emailMessage.getId(), emailMessage.getToEmail(), e);

            handleFailure(emailMessage, e);
        }
    }

    private void handleFailure(EmailMessageDto emailMessage, Exception e) {
        int currentRetry = emailMessage.getRetryCount();
        
        if (currentRetry < MAX_RETRIES) {
            emailMessage.setRetryCount(currentRetry + 1);
            
            log.info("Scheduling retry {} for email: {}", currentRetry + 1, emailMessage.getId());
            emailMessageSender.sendToRetry(emailMessage);
        } else {
            log.error("Max retries exceeded for email: {}. Sending to DLQ.", emailMessage.getId());
            emailMessageSender.sendToDlq(emailMessage);
        }
    }
}
