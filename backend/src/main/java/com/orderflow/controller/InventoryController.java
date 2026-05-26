package com.orderflow.controller;

import com.orderflow.dto.InventoryResponse;
import com.orderflow.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryResponse> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID productId) {
        InventoryResponse inventory = inventoryService.getInventory(productId);
        return ResponseEntity.ok(inventory);
    }
}
