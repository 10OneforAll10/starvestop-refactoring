package com.allforone.starvestop.domain.notification.service;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "fcm.exchange";
    public static final String QUEUE_NAME = "fcm.queue";
    public static final String ROUTING_KEY = "fcm.routing.key";

    public static final String DLX_NAME = "fcm.dlx.name";
    public static final String DLQ_NAME = "fcm.dlq";

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(ROUTING_KEY);
    }

    // 2. 메인 큐 설정 (NACK 발생 시 DLX로 라우팅되도록 x-dead-letter 설정)
    @Bean
    public Queue fcmQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange fcmExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding fcmBinding() {
        return BindingBuilder.bind(fcmQueue()).to(fcmExchange()).with(ROUTING_KEY);
    }

    // 3. 500건 청크 단위 소비를 위한 Batch 컨테이너 팩토리
    @Bean
    public SimpleRabbitListenerContainerFactory batchContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setBatchListener(true); // Batch(List) 형태로 수신 활성화
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(500); // 500개 단위로 청크 분리
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 수동 ACK (안전성 확보)
        return factory;
    }
}
