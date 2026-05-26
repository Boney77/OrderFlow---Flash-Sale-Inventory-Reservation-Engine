package com.orderflow.scheduler;

import com.orderflow.config.RedisKeys;
import com.orderflow.entity.Reservation;
import com.orderflow.entity.ReservationStatus;
import com.orderflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, Instant.now());

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Processing {} expired reservations", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            try {
                String inventoryKey = RedisKeys.inventoryKey(reservation.getProductId());
                redisTemplate.opsForValue().increment(inventoryKey, reservation.getQuantity());

                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);

                log.info("Expired reservation {} for product {} - restored {} units",
                        reservation.getId(),
                        reservation.getProductId(),
                        reservation.getQuantity());
            } catch (Exception e) {
                log.error("Failed to process expired reservation {}: {}",
                        reservation.getId(), e.getMessage(), e);
            }
        }
    }
}
