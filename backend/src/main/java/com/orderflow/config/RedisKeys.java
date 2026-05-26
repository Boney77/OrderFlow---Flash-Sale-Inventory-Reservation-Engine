package com.orderflow.config;

import java.util.UUID;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String INVENTORY_KEY = "inventory:%s";
    public static final String RESERVATION_KEY = "reservation:%s";
    public static final String STATS_TOTAL_REQUESTS = "stats:total_requests";
    public static final String STATS_SUCCESSFUL_ORDERS = "stats:successful_orders";
    public static final String STATS_FAILED_REQUESTS = "stats:failed_requests";

    public static String inventoryKey(UUID productId) {
        return String.format(INVENTORY_KEY, productId.toString());
    }

    public static String reservationKey(UUID token) {
        return String.format(RESERVATION_KEY, token.toString());
    }
}
