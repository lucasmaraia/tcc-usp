package com.emailservice.infrastructure.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DLX = "email.dlx";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.dead";

    public static final String RETRY_QUEUE = "email.retry.queue";
    public static final String RETRY_EXCHANGE = "email.retry.exchange";
    public static final String RETRY_ROUTING_KEY = "email.retry";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public org.springframework.amqp.core.Queue emailQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public org.springframework.amqp.core.DirectExchange emailExchange() {
        return new org.springframework.amqp.core.DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public org.springframework.amqp.core.Binding emailBinding(
            org.springframework.amqp.core.Queue emailQueue, 
            org.springframework.amqp.core.DirectExchange emailExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Queue retryQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 10000)
                .build();
    }

    @Bean
    public org.springframework.amqp.core.DirectExchange retryExchange() {
        return new org.springframework.amqp.core.DirectExchange(RETRY_EXCHANGE);
    }

    @Bean
    public org.springframework.amqp.core.Binding retryBinding(
            org.springframework.amqp.core.Queue retryQueue, 
            org.springframework.amqp.core.DirectExchange retryExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(retryQueue)
                .to(retryExchange)
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Queue emailDlq() {
        return new org.springframework.amqp.core.Queue(EMAIL_DLQ, true);
    }

    @Bean
    public org.springframework.amqp.core.DirectExchange emailDlx() {
        return new org.springframework.amqp.core.DirectExchange(EMAIL_DLX);
    }

    @Bean
    public org.springframework.amqp.core.Binding dlqBinding(
            org.springframework.amqp.core.Queue emailDlq, 
            org.springframework.amqp.core.DirectExchange emailDlx) {
        return org.springframework.amqp.core.BindingBuilder.bind(emailDlq)
                .to(emailDlx)
                .with(EMAIL_DLQ_ROUTING_KEY);
    }
}
