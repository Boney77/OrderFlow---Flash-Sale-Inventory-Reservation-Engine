package com.orderflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.config.RedisKeys;
import com.orderflow.dto.ReservationDTO;
import com.orderflow.entity.Reservation;
import com.orderflow.entity.ReservationStatus;
import com.orderflow.exception.ApiException;
import com.orderflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final int RESERVATION_TTL_MINUTES = 5;
    private static final int RESERVATION_TTL_SECONDS = RESERVATION_TTL_MINUTES * 60;

    @Transactional
    public Reservation createReservation(UUID productId, String userId, int quantity) {
        UUID reservationToken = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES);

        Reservation reservation = new Reservation();
        reservation.setProductId(productId);
        reservation.setUserId(userId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservationToken(reservationToken);
        reservation.setExpiresAt(expiresAt);
        reservation.setCreatedAt(Instant.now());

        Reservation saved = reservationRepository.save(reservation);
        log.info("Created reservation {} for product {} user {} qty {}",
                reservationToken, productId, userId, quantity);

        cacheReservation(saved);

        return saved;
    }

    public ReservationDTO getReservation(UUID token) {
        String cachedJson = stringRedisTemplate.opsForValue()
                .get(RedisKeys.reservationKey(token));

        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, ReservationDTO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached reservation {}, falling back to DB", token);
            }
        }

        Reservation reservation = reservationRepository.findByReservationToken(token)
                .orElseThrow(() -> ApiException.notFound("Reservation not found: " + token));

        return toDTO(reservation);
    }

    private void cacheReservation(Reservation reservation) {
        String key = RedisKeys.reservationKey(reservation.getReservationToken());
        ReservationDTO dto = toDTO(reservation);

        try {
            String json = objectMapper.writeValueAsString(dto);
            stringRedisTemplate.opsForValue().set(key, json, RESERVATION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Cached reservation {} with TTL {}s", reservation.getReservationToken(), RESERVATION_TTL_SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize reservation {} for caching", reservation.getReservationToken(), e);
        }
    }

    private ReservationDTO toDTO(Reservation reservation) {
        return ReservationDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus().name())
                .reservationToken(reservation.getReservationToken())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
