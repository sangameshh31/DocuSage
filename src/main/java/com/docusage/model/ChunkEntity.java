package com.docusage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * One chunk of a document's text, plus its embedding vector.
 * The embedding is stored as a comma-separated string of floats so the
 * in-memory {@link com.docusage.service.VectorIndex} can be rebuilt from
 * the database on startup. (A production system would use a native vector
 * column such as pgvector — see the README roadmap.)
 */
@Entity
@Table(name = "chunks")
public class ChunkEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false, length = 20000)
    private String text;

    @Lob
    @Column(nullable = false)
    private String embedding;

    public ChunkEntity() {
    }

    public ChunkEntity(String id, String documentId, int chunkIndex, String text, String embedding) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.embedding = embedding;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
}
