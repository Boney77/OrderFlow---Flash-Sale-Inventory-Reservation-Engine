package com.orderflow.websocket;

import com.orderflow.config.RedisKeys;
import com.orderflow.dto.DashboardStats;
import com.orderflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    private final AtomicLong previousSuccessfulOrders = new AtomicLong(0);
    private volatile long lastUpdateTime = System.currentTimeMillis();

    @Scheduled(fixedRate = 1000)
    public void publishDashboardStats() {
        try {
            DashboardStats stats = buildDashboardStats();
            messagingTemplate.convertAndSend("/topic/dashboard", stats);
            log.debug("Published dashboard stats: {}", stats);
        } catch (Exception e) {
            log.error("Failed to publish dashboard stats", e);
        }
    }

    private DashboardStats buildDashboardStats() {
        long stockRemaining = calculateTotalStockRemaining();
        long totalRequests = getRedisCounter(RedisKeys.STATS_TOTAL_REQUESTS);
        long successfulOrders = getRedisCounter(RedisKeys.STATS_SUCCESSFUL_ORDERS);
        long failedRequests = getRedisCounter(RedisKeys.STATS_FAILED_REQUESTS);
        double ordersPerSecond = calculateOrdersPerSecond(successfulOrders);

        return DashboardStats.builder()
                .stockRemaining(stockRemaining)
                .totalRequests(totalRequests)
                .successfulOrders(successfulOrders)
                .failedRequests(failedRequests)
                .ordersPerSecond(ordersPerSecond)
                .build();
    }

    private long calculateTotalStockRemaining() {
        return productRepository.findAll().stream()
                .mapToLong(product -> {
                    String key = RedisKeys.inventoryKey(product.getId());
                    String value = redisTemplate.opsForValue().get(key);
                    return value != null ? Long.parseLong(value) : 0L;
                })
                .sum();
    }

    private long getRedisCounter(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private double calculateOrdersPerSecond(long currentSuccessfulOrders) {
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastUpdateTime;

        if (timeDelta <= 0) {
            return 0.0;
        }

        long previousCount = previousSuccessfulOrders.getAndSet(currentSuccessfulOrders);
        long ordersDelta = currentSuccessfulOrders - previousCount;
        lastUpdateTime = currentTime;

        return (ordersDelta * 1000.0) / timeDelta;
    }
}
