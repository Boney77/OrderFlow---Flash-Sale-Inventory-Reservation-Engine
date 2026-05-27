package com.orderflow.config;

import com.orderflow.entity.Product;
import com.orderflow.repository.ProductRepository;
import com.orderflow.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting inventory sync to Redis...");
        
        List<Product> products = productRepository.findAll();
        
        if (products.isEmpty()) {
            log.warn("No products found in database to sync");
            return;
        }

        int synced = 0;
        for (Product product : products) {
            try {
                inventoryService.syncToRedis(product.getId(), product.getAvailableStock());
                synced++;
            } catch (Exception ex) {
                log.error(
                        "Failed to sync product {} to Redis: {}",
                        product.getId(),
                        ex.getMessage());
            }
        }

        log.info("Inventory sync completed. Synced {} of {} products to Redis.", synced, products.size());
    }
}
