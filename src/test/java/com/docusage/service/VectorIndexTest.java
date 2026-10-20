package com.docusage.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorIndexTest {

    private VectorIndex.IndexedChunk chunk(String id, String docId, int idx, float[] vector) {
        return new VectorIndex.IndexedChunk(id, docId, idx, "text-" + id, vector);
    }

    @Test
    void identicalVectorScoresOne() {
        assertEquals(1.0, VectorIndex.cosine(new float[]{1, 0}, new float[]{1, 0}), 1e-9);
    }

    @Test
    void orthogonalVectorScoresZero() {
        assertEquals(0.0, VectorIndex.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-9);
    }

    @Test
    void searchReturnsClosestFirst() {
        VectorIndex index = new VectorIndex();
        index.add(chunk("a", "doc1", 0, new float[]{1, 0}));
        index.add(chunk("b", "doc1", 1, new float[]{0.7f, 0.7f}));
        index.add(chunk("c", "doc1", 2, new float[]{0, 1}));

        List<VectorIndex.ScoredChunk> hits = index.search(new float[]{1, 0.1f}, 2, null);
        assertEquals(2, hits.size());
        assertEquals("a", hits.get(0).chunk().chunkId());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }

    @Test
    void searchCanBeScopedToOneDocument() {
        VectorIndex index = new VectorIndex();
        index.add(chunk("a", "doc1", 0, new float[]{1, 0}));
        index.add(chunk("b", "doc2", 0, new float[]{1, 0}));

        List<VectorIndex.ScoredChunk> hits = index.search(new float[]{1, 0}, 10, "doc2");
        assertEquals(1, hits.size());
        assertEquals("b", hits.get(0).chunk().chunkId());
    }

    @Test
    void removeByDocumentDropsItsChunks() {
        VectorIndex index = new VectorIndex();
        index.add(chunk("a", "doc1", 0, new float[]{1, 0}));
        index.add(chunk("b", "doc1", 1, new float[]{0, 1}));
        index.add(chunk("c", "doc2", 0, new float[]{1, 1}));
        index.removeByDocument("doc1");
        assertEquals(1, index.size());
    }

    @Test
    void serializeDeserializeRoundTrips() {
        float[] original = {0.5f, -1.25f, 3.0f, 0.0f};
        float[] restored = VectorIndex.deserialize(VectorIndex.serialize(original));
        assertEquals(original.length, restored.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], restored[i], 1e-6);
        }
    }

    @Test
    void dimensionMismatchIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorIndex.cosine(new float[]{1, 0}, new float[]{1, 0, 0}));
    }
}
