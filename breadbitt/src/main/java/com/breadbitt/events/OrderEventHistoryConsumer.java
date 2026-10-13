
package com.breadbitt.events;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

@Component
public class OrderEventHistoryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventHistoryConsumer.class);

    private final CqlSession cassandraSession;
    private final PreparedStatement insertStatement;

    public OrderEventHistoryConsumer(CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;

        this.insertStatement = cassandraSession.prepare(
                "INSERT INTO order_event_history " +
                        "(order_id, event_time, event_id, event_type, " +
                        "event_status, event_details) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
    }

    @KafkaListener(topics = "${breadbite.kafka.order-topic:breadbite.orders.placed}", groupId = "${breadbite.kafka.cassandra-group:breadbite-cassandra-history}")
    public void saveOrderEvent(OrderPlacedEvent event) {

        if (event == null || event.getOrderId() == null) {
            throw new IllegalArgumentException(
                    "OrderPlacedEvent must contain an order ID");
        }

        if (event.getEventId() == null ||
                event.getEventId().isBlank()) {
            throw new IllegalArgumentException(
                    "OrderPlacedEvent must contain an event ID");
        }

        UUID eventId = UUID.fromString(event.getEventId());

        Instant occurredAt = event.getOccurredAt();

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "OrderPlacedEvent must contain occurredAt");
        }

        String details = "Order total: " + event.getOrderTotal();

        cassandraSession.execute(
                insertStatement.bind(
                        event.getOrderId(),
                        occurredAt,
                        eventId,
                        "ORDER_PLACED",
                        event.getOrderStatus(),
                        details));

        logger.info(
                "Saved Cassandra order event history for order {}",
                event.getOrderId());
    }
}
