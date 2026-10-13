
package com.breadbitt.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPlacedKafkaHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderPlacedKafkaHandler.class);

    private final OrderEventPublisher orderEventPublisher;

    public OrderPlacedKafkaHandler(
            OrderEventPublisher orderEventPublisher) {

        this.orderEventPublisher = orderEventPublisher;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleOrderPlaced(OrderPlacedEvent event) {

        logger.info(
                "Order transaction committed. Sending Kafka event for order {}",
                event.getOrderId()
        );

        orderEventPublisher.publish(event);
    }
}