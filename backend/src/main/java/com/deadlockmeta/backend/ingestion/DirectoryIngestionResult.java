package com.deadlockmeta.backend.ingestion;

public record DirectoryIngestionResult(
                int filesProcessed,
                int matchesInserted,
                int playersInserted,
                int itemPurchasesInserted) {
}