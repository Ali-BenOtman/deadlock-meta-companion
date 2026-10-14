package com.deadlockmeta.backend.extraction;

public record ExtractionResult(
        String file,
        int filesRead,
        long sourceBytes,
        long rows,
        long bytes,
        double seconds) {
}