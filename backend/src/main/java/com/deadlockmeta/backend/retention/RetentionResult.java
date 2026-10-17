package com.deadlockmeta.backend.retention;

public record RetentionResult(
        long matchesDeleted,
        long playersDeleted,
        long itemPurchasesDeleted,
        double seconds) {
}