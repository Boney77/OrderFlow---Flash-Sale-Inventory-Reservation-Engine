package com.orderflow.service;

import com.orderflow.config.RedisKeys;
import com.orderflow.dto.PurchaseRequest;
import com.orderflow.dto.PurchaseResponse;
import com.orderflow.entity.Reservation;
import com.orderflow.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ReservationService reservationService;

    private static final String LUA_ATOMIC_DECREMENT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock < tonumber(ARGV[1]) then return -1 end
            return redis.call('DECRBY', KEYS[1], ARGV[1])
            """;

    private final RedisScript<Long> atomicDecrementScript = new DefaultRedisScript<>(LUA_ATOMIC_DECREMENT, Long.class);

    public PurchaseResponse attemptPurchase(PurchaseRequest request) {
        log.info("Attempting purchase: product={}, user={}, qty={}",
                request.getProductId(), request.getUserId(), request.getQuantity());

        incrementTotalRequests();

        String inventoryKey = RedisKeys.inventoryKey(request.getProductId());
        List<String> keys = Collections.singletonList(inventoryKey);

        Long result = stringRedisTemplate.execute(
                atomicDecrementScript,
                keys,
                String.valueOf(request.getQuantity())
        );

        if (result == null || result == -1) {
            log.info("Purchase failed - sold out: product={}, user={}",
                    request.getProductId(), request.getUserId());
            incrementFailedRequests();
            throw ApiException.conflict("SOLD_OUT");
        }

        log.info("Stock decremented atomically: product={}, remaining={}", 
                request.getProductId(), result);

        Reservation reservation = reservationService.createReservation(
                request.getProductId(),
                request.getUserId(),
                request.getQuantity()
        );

        incrementSuccessfulOrders();

        return PurchaseResponse.builder()
                .reservationToken(reservation.getReservationToken())
                .expiresAt(reservation.getExpiresAt())
                .message("Reservation created successfully. Complete your order within 5 minutes.")
                .build();
    }

    private void incrementTotalRequests() {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.STATS_TOTAL_REQUESTS);
        } catch (Exception e) {
            log.warn("Failed to increment total requests counter", e);
        }
    }

    private void incrementSuccessfulOrders() {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.STATS_SUCCESSFUL_ORDERS);
        } catch (Exception e) {
            log.warn("Failed to increment successful orders counter", e);
        }
    }

    private void incrementFailedRequests() {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.STATS_FAILED_REQUESTS);
        } catch (Exception e) {
            log.warn("Failed to increment failed requests counter", e);
        }
    }
}
