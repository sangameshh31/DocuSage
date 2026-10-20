package com.docusage.service.chat;

import com.docusage.service.VectorIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Zero-config answer provider: instead of generating prose, it answers
 * <i>extractively</i> — it returns the most relevant retrieved passages
 * verbatim, ranked by similarity. That is honest about what it is (no
 * hallucination is possible), needs no API key, and makes the retrieved
 * citations the product. Switch to the Gemini provider for generated,
 * natural-language answers over the same retrieval.
 */
@Component
@ConditionalOnProperty(name = "docusage.chat.provider", havingValue = "extractive", matchIfMissing = true)
public class ExtractiveChatProvider implements ChatProvider {

    @Override
    public String answer(String question, List<VectorIndex.ScoredChunk> contexts) {
        if (contexts.isEmpty()) {
            return "No relevant passage was found in the indexed documents for that question. "
                    + "Try uploading a document first, or rephrasing the question.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here are the most relevant passages from your documents ")
                .append("(switch docusage.chat.provider=gemini for a generated answer):\n\n");
        for (int i = 0; i < contexts.size(); i++) {
            VectorIndex.ScoredChunk hit = contexts.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append(hit.chunk().text().trim())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String name() {
        return "extractive";
    }
}
