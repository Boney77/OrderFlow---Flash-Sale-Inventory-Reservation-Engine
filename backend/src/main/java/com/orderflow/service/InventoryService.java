package com.orderflow.service;

import com.orderflow.config.RedisKeys;
import com.orderflow.dto.InventoryResponse;
import com.orderflow.entity.Product;
import com.orderflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductRepository productRepository;

    public void syncToRedis(UUID productId, int stock) {
        String key = RedisKeys.inventoryKey(productId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("Synced inventory to Redis: {} = {}", key, stock);
    }

    public int getStockFromRedis(UUID productId) {
        String key = RedisKeys.inventoryKey(productId);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            log.warn("No inventory found in Redis for product: {}", productId);
            return 0;
        }
        return Integer.parseInt(value);
    }

    public InventoryResponse getInventory(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        int redisStock = getStockFromRedis(productId);

        return InventoryResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .totalStock(product.getTotalStock())
                .availableStock(redisStock)
                .build();
    }

    public List<InventoryResponse> getAllInventory() {
        return productRepository.findAll().stream()
                .map(product -> InventoryResponse.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .totalStock(product.getTotalStock())
                        .availableStock(getStockFromRedis(product.getId()))
                        .build())
                .toList();
    }
}
