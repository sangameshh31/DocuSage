package com.docusage.service.embedding;

import com.docusage.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Real semantic embeddings from Google's Gemini API
 * (default model: text-embedding-004, 768 dimensions).
 *
 * <p>Enabled with {@code docusage.embedding.provider=gemini} and a
 * {@code GEMINI_API_KEY}. Talks to the API with the JDK's built-in
 * HttpClient — no vendor SDK needed for one REST call.
 */
@Component
@ConditionalOnProperty(name = "docusage.embedding.provider", havingValue = "gemini")
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";

    private final AppProperties properties;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiEmbeddingProvider(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set, but docusage.embedding.provider=gemini");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "content", Map.of("parts", new Object[]{Map.of("text", text)})
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(properties.getGemini().getEmbeddingModel(), apiKey)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Gemini embedding call failed: HTTP "
                        + response.statusCode() + " " + response.body());
            }
            JsonNode values = mapper.readTree(response.body())
                    .path("embedding").path("values");
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            HashingEmbeddingProvider.normalize(vector);
            return vector;
        } catch (IOException e) {
            throw new IllegalStateException("Gemini embedding call failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini embedding call interrupted", e);
        }
    }

    @Override
    public String name() {
        return "gemini(" + properties.getGemini().getEmbeddingModel() + ")";
    }
}
