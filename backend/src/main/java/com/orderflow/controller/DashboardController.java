package com.orderflow.controller;

import com.orderflow.config.RedisKeys;
import com.orderflow.dto.DashboardStats;
import com.orderflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    @GetMapping("/dashboard/stats")
    public DashboardStats getDashboardStats() {
        long stockRemaining = calculateTotalStockRemaining();
        long totalRequests = getRedisCounter(RedisKeys.STATS_TOTAL_REQUESTS);
        long successfulOrders = getRedisCounter(RedisKeys.STATS_SUCCESSFUL_ORDERS);
        long failedRequests = getRedisCounter(RedisKeys.STATS_FAILED_REQUESTS);

        return DashboardStats.builder()
                .stockRemaining(stockRemaining)
                .totalRequests(totalRequests)
                .successfulOrders(successfulOrders)
                .failedRequests(failedRequests)
                .ordersPerSecond(0.0)
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
}
