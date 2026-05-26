package com.orderflow.controller;

import com.orderflow.dto.ConfirmOrderRequest;
import com.orderflow.dto.OrderDTO;
import com.orderflow.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/confirm-order")
    public ResponseEntity<OrderDTO> confirmOrder(@Valid @RequestBody ConfirmOrderRequest request) {
        OrderDTO order = orderService.confirmOrder(request);
        return ResponseEntity.ok(order);
    }
}
