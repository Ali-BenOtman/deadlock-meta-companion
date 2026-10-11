package com.deadlockmeta.backend.ingestion;

public record IngestionResult(int matchesInserted, int playersInserted, int itemPurchasesInserted) {
}