package com.docusage.service.chat;

import com.docusage.config.AppProperties;
import com.docusage.service.VectorIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Generated answers from Google's Gemini API, grounded in the retrieved
 * chunks: the prompt instructs the model to answer using only the provided
 * context and to cite passages as [1], [2], ... — the same citation list
 * the API returns alongside the answer. Grounding + citations is what makes
 * this RAG rather than a bare chatbot: the model can only say what the
 * documents support.
 *
 * <p>Enabled with {@code docusage.chat.provider=gemini} and a
 * {@code GEMINI_API_KEY}.
 */
@Component
@ConditionalOnProperty(name = "docusage.chat.provider", havingValue = "gemini")
public class GeminiChatProvider implements ChatProvider {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String PROMPT_TEMPLATE = """
            Answer the question using ONLY the context passages below.
            Cite the passages you use with their numbers, like [1] or [2].
            If the context does not contain the answer, say exactly:
            "The documents do not contain that information."

            Context:
            %s

            Question: %s
            """;

    private final AppProperties properties;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiChatProvider(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String answer(String question, List<VectorIndex.ScoredChunk> contexts) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set, but docusage.chat.provider=gemini");
        }
        if (contexts.isEmpty()) {
            return "The documents do not contain that information.";
        }
        StringBuilder contextText = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            contextText.append("[").append(i + 1).append("] ")
                    .append(contexts.get(i).chunk().text().trim()).append("\n\n");
        }
        String prompt = PROMPT_TEMPLATE.formatted(contextText.toString().trim(), question);
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)})}
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(properties.getGemini().getChatModel(), apiKey)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Gemini chat call failed: HTTP "
                        + response.statusCode() + " " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            return root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("The documents do not contain that information.");
        } catch (IOException e) {
            throw new IllegalStateException("Gemini chat call failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini chat call interrupted", e);
        }
    }

    @Override
    public String name() {
        return "gemini(" + properties.getGemini().getChatModel() + ")";
    }
}
