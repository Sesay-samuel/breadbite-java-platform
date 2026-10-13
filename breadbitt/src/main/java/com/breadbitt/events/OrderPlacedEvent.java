
package com.breadbitt.events;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderPlacedEvent {

    private String eventId;
    private Long orderId;
    private BigDecimal orderTotal;
    private String orderStatus;
    private Instant occurredAt;

    public OrderPlacedEvent() {
    }

    public OrderPlacedEvent(
            String eventId,
            Long orderId,
            BigDecimal orderTotal,
            String orderStatus,
            Instant occurredAt) {

        this.eventId = eventId;
        this.orderId = orderId;
        this.orderTotal = orderTotal;
        this.orderStatus = orderStatus;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}