package com.orderflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private UUID id;
    private UUID reservationId;
    private UUID productId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;
}
