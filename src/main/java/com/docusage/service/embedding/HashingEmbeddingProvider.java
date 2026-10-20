package com.docusage.service.embedding;

import com.docusage.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Zero-config local embeddings using <b>feature hashing</b> (the "hashing
 * trick"): every token (and adjacent token pair) is hashed into a fixed-size
 * vector, with the hash's sign deciding whether to add or subtract. Texts
 * that share vocabulary land close together in vector space, which is enough
 * for keyword-style retrieval with no API key and no model download.
 *
 * <p>Be clear about the trade-off: this captures <i>lexical</i> overlap, not
 * true semantics — "car" and "automobile" won't match. For semantic quality,
 * switch to the Gemini provider. The interface exists precisely so that
 * upgrade is a config change, not a rewrite.
 */
@Component
@ConditionalOnProperty(name = "docusage.embedding.provider", havingValue = "hashing", matchIfMissing = true)
public class HashingEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public HashingEmbeddingProvider(AppProperties properties) {
        this.dimension = properties.getEmbedding().getDimension();
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        String[] tokens = tokenize(text);
        for (int i = 0; i < tokens.length; i++) {
            addToken(vector, tokens[i]);
            if (i + 1 < tokens.length) {
                // Bigrams give a little word-order/context signal.
                addToken(vector, tokens[i] + "_" + tokens[i + 1]);
            }
        }
        normalize(vector);
        return vector;
    }

    private void addToken(float[] vector, String token) {
        int hash = token.hashCode();
        int index = Math.floorMod(hash, dimension);
        float sign = (hash & 1) == 0 ? 1f : -1f;
        vector[index] += sign;
    }

    static void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += (double) v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }

    static String[] tokenize(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
    }

    @Override
    public String name() {
        return "hashing(dim=" + dimension + ")";
    }
}
