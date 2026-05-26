package com.orderflow.service;

import com.orderflow.config.RabbitMQConfig;
import com.orderflow.config.RedisKeys;
import com.orderflow.dto.ConfirmOrderRequest;
import com.orderflow.dto.OrderDTO;
import com.orderflow.entity.Order;
import com.orderflow.entity.OrderStatus;
import com.orderflow.entity.Reservation;
import com.orderflow.entity.ReservationStatus;
import com.orderflow.event.OrderConfirmedEvent;
import com.orderflow.exception.ApiException;
import com.orderflow.repository.OrderRepository;
import com.orderflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final BigDecimal SIMULATED_PRICE = new BigDecimal("99.99");

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderDTO confirmOrder(ConfirmOrderRequest request) {
        Reservation reservation = reservationRepository
                .findByReservationToken(request.getReservationToken())
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESERVATION"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw ApiException.badRequest("INVALID_RESERVATION");
        }

        if (reservation.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("INVALID_RESERVATION");
        }

        BigDecimal amount = SIMULATED_PRICE.multiply(BigDecimal.valueOf(reservation.getQuantity()));

        Order order = new Order();
        order.setReservationId(reservation.getId());
        order.setProductId(reservation.getProductId());
        order.setUserId(reservation.getUserId());
        order.setAmount(amount);
        order.setStatus(OrderStatus.CONFIRMED);

        order = orderRepository.save(order);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        redisTemplate.delete(RedisKeys.reservationKey(reservation.getReservationToken()));

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                order.getId(),
                reservation.getId(),
                reservation.getProductId(),
                reservation.getUserId(),
                amount,
                Instant.now()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_CONFIRMED_QUEUE, event);

        log.info("Order {} confirmed for reservation {}", order.getId(), reservation.getId());

        return OrderDTO.builder()
                .id(order.getId())
                .reservationId(order.getReservationId())
                .productId(order.getProductId())
                .userId(order.getUserId())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
