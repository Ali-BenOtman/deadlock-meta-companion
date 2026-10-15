package com.deadlockmeta.backend.update;

public record DataUpdateResult(
        String status,
        int newFiles,
        int matchesInserted,
        int playersInserted,
        int itemPurchasesInserted,
        boolean statsRefreshed,
        double seconds) {
}