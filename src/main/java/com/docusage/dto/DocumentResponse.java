package com.docusage.dto;

import com.docusage.model.DocumentEntity;

import java.time.Instant;

/** API view of a document's metadata. */
public record DocumentResponse(
        String id,
        String name,
        String contentType,
        long sizeBytes,
        int chunkCount,
        Instant createdAt
) {

    public static DocumentResponse from(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(), entity.getName(), entity.getContentType(),
                entity.getSizeBytes(), entity.getChunkCount(), entity.getCreatedAt());
    }
}
