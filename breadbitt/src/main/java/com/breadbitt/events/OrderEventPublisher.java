
package com.breadbitt.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
            @Value("${breadbite.kafka.order-topic:breadbite.orders.placed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderPlacedEvent event) {
        kafkaTemplate.send(
                topic,
                String.valueOf(event.getOrderId()),
                event).whenComplete((result, error) -> {
                    if (error != null) {
                        org.slf4j.LoggerFactory
                                .getLogger(OrderEventPublisher.class)
                                .error(
                                        "Failed to publish OrderPlaced event for order {}",
                                        event.getOrderId(),
                                        error);
                    } else {
                        org.slf4j.LoggerFactory
                                .getLogger(OrderEventPublisher.class)
                                .info(
                                        "Published OrderPlaced event for order {}",
                                        event.getOrderId());
                    }
                });
    }
}