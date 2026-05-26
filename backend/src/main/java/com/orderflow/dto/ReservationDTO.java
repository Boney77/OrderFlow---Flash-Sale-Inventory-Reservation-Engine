package com.orderflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDTO {

    private UUID id;
    private String userId;
    private UUID productId;
    private int quantity;
    private String status;
    private UUID reservationToken;
    private Instant expiresAt;
    private Instant createdAt;
}
