package com.docusage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Metadata for one uploaded document. The text itself lives in {@link ChunkEntity} rows. */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String contentType;

    private long sizeBytes;

    private int chunkCount;

    @Column(nullable = false)
    private Instant createdAt;

    public DocumentEntity() {
    }

    public DocumentEntity(String id, String name, String contentType, long sizeBytes,
                          int chunkCount, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.chunkCount = chunkCount;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
