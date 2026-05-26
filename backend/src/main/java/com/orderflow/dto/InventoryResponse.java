package com.orderflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private UUID productId;
    private String productName;
    private int totalStock;
    private int availableStock;
}
