
package com.breadbitt.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OrderEventPublisherTest {

    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private OrderEventPublisher publisher;

    private static final String TOPIC = "breadbite.orders.placed";

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new OrderEventPublisher(kafkaTemplate, TOPIC);
    }

    @Test
    void shouldPublishOrderPlacedEventToKafka() {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "test-event-001",
                1001L,
                new BigDecimal("19.99"),
                "created",
                Instant.parse("2026-10-12T18:00:00Z"));

        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(TOPIC, "1001", event))
                .thenReturn(future);

        assertDoesNotThrow(() -> publisher.publish(event));

        verify(kafkaTemplate, times(1))
                .send(TOPIC, "1001", event);

        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void shouldHandleKafkaPublishFailureWithoutThrowing() {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "test-event-002",
                1002L,
                new BigDecimal("25.50"),
                "created",
                Instant.parse("2026-10-12T18:05:00Z"));

        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(TOPIC, "1002", event))
                .thenReturn(future);

        assertDoesNotThrow(() -> publisher.publish(event));

        verify(kafkaTemplate, times(1))
                .send(TOPIC, "1002", event);
    }

    @Test
    void shouldRejectNullEvent() {
        assertThrows(
                NullPointerException.class,
                () -> publisher.publish(null));

        verifyNoInteractions(kafkaTemplate);
    }
}
