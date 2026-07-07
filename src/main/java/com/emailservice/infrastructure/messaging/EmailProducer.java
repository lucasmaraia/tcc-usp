package com.emailservice.infrastructure.messaging;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.port.EmailQueueGateway;
import com.emailservice.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailProducer implements EmailQueueGateway {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendEmail(EmailMessageDto emailMessage) {
        log.info("Publishing email {} for recipient {}", emailMessage.id(), emailMessage.toEmail());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailMessage
        );
    }

    @Override
    public void sendToRetry(EmailMessageDto emailMessage) {
        log.info("Publishing email {} to retry queue (attempt {})", emailMessage.id(), emailMessage.retryCount());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RETRY_EXCHANGE,
                RabbitMQConfig.RETRY_ROUTING_KEY,
                emailMessage
        );
    }

    @Override
    public void sendToDlq(EmailMessageDto emailMessage) {
        log.warn("Publishing email {} to DLQ", emailMessage.id());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_DLX,
                RabbitMQConfig.EMAIL_DLQ_ROUTING_KEY,
                emailMessage
        );
    }
}
