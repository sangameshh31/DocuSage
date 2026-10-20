package com.docusage.service.chat;

import com.docusage.service.VectorIndex;

import java.util.List;

/**
 * Turns a question plus the retrieved chunks into an answer.
 * Selected with {@code docusage.chat.provider}; the local extractive
 * provider needs no API key, the Gemini provider writes a fluent answer
 * grounded in the same retrieved chunks.
 */
public interface ChatProvider {

    String answer(String question, List<VectorIndex.ScoredChunk> contexts);

    /** Human-readable provider name, surfaced by /api/health. */
    String name();
}
