package com.chaiyanan09.thairag.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        DocumentStatus status,
        int pageCount,
        int chunkCount,
        List<Integer> emptyPages,
        String errorMessage,
        Instant createdAt
) {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getFilename(),
                d.getStatus(),
                d.getPageCount(),
                d.getChunkCount(),
                List.copyOf(d.getEmptyPages()),
                d.getErrorMessage(),
                d.getCreatedAt());
    }
}
