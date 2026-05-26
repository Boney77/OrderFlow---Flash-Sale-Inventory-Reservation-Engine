package com.orderflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    private long stockRemaining;
    private long totalRequests;
    private long successfulOrders;
    private long failedRequests;
    private double ordersPerSecond;
}
