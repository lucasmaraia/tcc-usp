package com.emailservice.infrastructure.messaging;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailProducer implements EmailMessageSender {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendEmail(EmailMessageDto emailMessage) {
        log.info("Sending email message to queue: {} for recipient: {}", 
                emailMessage.getId(), emailMessage.getToEmail());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailMessage
        );
    }

    @Override
    public void sendToRetry(EmailMessageDto emailMessage) {
        log.info("Sending email message to retry queue: {} for recipient: {}", 
                emailMessage.getId(), emailMessage.getToEmail());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RETRY_EXCHANGE,
                RabbitMQConfig.RETRY_ROUTING_KEY,
                emailMessage
        );
    }

    @Override
    public void sendToDlq(EmailMessageDto emailMessage) {
        log.info("Sending email message to DLQ: {} for recipient: {}",
                emailMessage.getId(), emailMessage.getToEmail());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_DLX,
                RabbitMQConfig.EMAIL_DLQ_ROUTING_KEY,
                emailMessage
        );
    }
}
