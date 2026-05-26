package com.orderflow.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderConfirmedEvent {

    private UUID orderId;
    private UUID reservationId;
    private UUID productId;
    private String userId;
    private BigDecimal amount;
    private Instant timestamp;
}
