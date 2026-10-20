package com.docusage.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A hand-rolled in-memory vector index — the "database" half of retrieval.
 *
 * <p>Every chunk's embedding lives in a map keyed by chunk id. A search
 * embeds the question, computes the <b>cosine similarity</b> against every
 * stored vector, and keeps the top-k with a bounded min-heap. Because all
 * vectors are L2-normalized at embed time, cosine similarity is just a dot
 * product.
 *
 * <p>This is an exact (brute-force) search: O(n) per query, perfectly fine
 * up to tens of thousands of chunks, and exactly correct — unlike approximate
 * indexes (HNSW, IVF) which trade a little accuracy for speed at millions of
 * vectors. The README roadmap covers when and how to graduate to those.
 */
@Component
public class VectorIndex {

    /** One stored chunk: its text plus the vector we search over. */
    public record IndexedChunk(String chunkId, String documentId, int chunkIndex,
                               String text, float[] vector) {
    }

    /** A search hit: the chunk plus its similarity score in [-1, 1]. */
    public record ScoredChunk(IndexedChunk chunk, double score) {
    }

    private final Map<String, IndexedChunk> chunks = new ConcurrentHashMap<>();

    public void add(IndexedChunk chunk) {
        chunks.put(chunk.chunkId(), chunk);
    }

    public void removeByDocument(String documentId) {
        chunks.values().removeIf(c -> c.documentId().equals(documentId));
    }

    public void clear() {
        chunks.clear();
    }

    public int size() {
        return chunks.size();
    }

    /**
     * Top-k most similar chunks to the query vector.
     *
     * @param documentId if non-null, restrict the search to one document
     */
    public List<ScoredChunk> search(float[] queryVector, int topK, String documentId) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (IndexedChunk chunk : chunks.values()) {
            if (documentId != null && !chunk.documentId().equals(documentId)) {
                continue;
            }
            scored.add(new ScoredChunk(chunk, cosine(queryVector, chunk.vector())));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() <= topK ? scored : new ArrayList<>(scored.subList(0, topK));
    }

    /** Cosine similarity between two equal-length vectors, in [-1, 1]. */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vector dimension mismatch: " + a.length + " vs " + b.length
                            + " (was the embedding provider changed after documents were indexed?)");
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Serialize a vector for database storage. */
    public static String serialize(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    /** Parse a vector stored by {@link #serialize(float[])}. */
    public static float[] deserialize(String stored) {
        String[] parts = stored.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
