package com.orderflow.controller;

import com.orderflow.dto.PurchaseRequest;
import com.orderflow.dto.PurchaseResponse;
import com.orderflow.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> purchase(@Valid @RequestBody PurchaseRequest request) {
        log.debug("Received purchase request: {}", request);
        PurchaseResponse response = purchaseService.attemptPurchase(request);
        return ResponseEntity.ok(response);
    }
}
