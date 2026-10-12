
package com.breadbitt.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPlacedListener.class);

    @KafkaListener(topics = "${breadbite.kafka.order-topic:breadbite.orders.placed}", groupId = "breadbite-order-notifications")
    public void onOrderPlaced(OrderPlacedEvent event) {

        logger.info(
                "Received OrderPlaced event: eventId={}, orderId={}, status={}",
                event.getEventId(),
                event.getOrderId(),
                event.getOrderStatus());

        // Later: send confirmation emails or update an audit log.
    }
}
