package com.docusage.service.embedding;

import com.docusage.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashingEmbeddingProviderTest {

    private final HashingEmbeddingProvider provider = new HashingEmbeddingProvider(new AppProperties());

    @Test
    void producesConfiguredDimension() {
        float[] vector = provider.embed("hello world");
        assertEquals(384, vector.length);
    }

    @Test
    void vectorsAreNormalized() {
        float[] vector = provider.embed("the quick brown fox");
        double norm = 0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        assertEquals(1.0, Math.sqrt(norm), 1e-5);
    }

    @Test
    void similarTextsAreCloserThanUnrelatedTexts() {
        float[] query = provider.embed("reset my password and login help");
        float[] similar = provider.embed("how do I reset my password for login");
        float[] unrelated = provider.embed("the recipe calls for two cups of flour");
        double simScore = com.docusage.service.VectorIndex.cosine(query, similar);
        double unrelScore = com.docusage.service.VectorIndex.cosine(query, unrelated);
        assertTrue(simScore > unrelScore,
                "expected similar text to score higher: " + simScore + " vs " + unrelScore);
    }

    @Test
    void embeddingIsDeterministic() {
        float[] a = provider.embed("same input");
        float[] b = provider.embed("same input");
        assertEquals(1.0, com.docusage.service.VectorIndex.cosine(a, b), 1e-6);
    }
}
