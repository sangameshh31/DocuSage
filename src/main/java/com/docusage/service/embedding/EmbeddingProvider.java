package com.docusage.service.embedding;

/**
 * Turns text into a vector (embedding). Implementations are selected with
 * {@code docusage.embedding.provider} — the rest of the system only ever
 * sees this interface, so swapping the local hashing provider for a real
 * embedding model changes nothing else.
 */
public interface EmbeddingProvider {

    /** Embed one piece of text. The returned vector is L2-normalized. */
    float[] embed(String text);

    /** Human-readable provider name, surfaced by /api/health. */
    String name();
}
