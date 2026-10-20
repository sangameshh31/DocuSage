package com.docusage.service;

import com.docusage.config.AppProperties;
import com.docusage.dto.AskResponse;
import com.docusage.model.DocumentEntity;
import com.docusage.repository.DocumentRepository;
import com.docusage.service.chat.ChatProvider;
import com.docusage.service.embedding.EmbeddingProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The query pipeline — the "R" and "G" of RAG:
 * embed the question → retrieve the top-k chunks from the vector index →
 * hand question + chunks to the chat provider → return the answer with
 * the exact citations it was grounded in.
 */
@Service
public class QaService {

    private final EmbeddingProvider embeddingProvider;
    private final ChatProvider chatProvider;
    private final VectorIndex index;
    private final DocumentRepository documentRepository;
    private final AppProperties properties;

    public QaService(EmbeddingProvider embeddingProvider,
                     ChatProvider chatProvider,
                     VectorIndex index,
                     DocumentRepository documentRepository,
                     AppProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.chatProvider = chatProvider;
        this.index = index;
        this.documentRepository = documentRepository;
        this.properties = properties;
    }

    public AskResponse ask(String question, String documentId) {
        if (documentId != null && !documentId.isBlank()) {
            // Fail fast with a 404 instead of silently searching everything.
            documentRepository.findById(documentId)
                    .orElseThrow(() -> new com.docusage.exception.NotFoundException(
                            "Document not found: " + documentId));
        }
        float[] queryVector = embeddingProvider.embed(question);
        List<VectorIndex.ScoredChunk> hits = index.search(queryVector, properties.getTopK(),
                (documentId == null || documentId.isBlank()) ? null : documentId);

        String answer = chatProvider.answer(question, hits);

        List<AskResponse.Citation> citations = hits.stream()
                .map(hit -> new AskResponse.Citation(
                        hit.chunk().documentId(),
                        documentName(hit.chunk().documentId()),
                        hit.chunk().chunkIndex(),
                        Math.round(hit.score() * 10000.0) / 10000.0,
                        excerpt(hit.chunk().text())))
                .toList();

        return new AskResponse(answer, citations, embeddingProvider.name(), chatProvider.name());
    }

    private String documentName(String documentId) {
        return documentRepository.findById(documentId)
                .map(DocumentEntity::getName)
                .orElse(documentId);
    }

    /** Keep citation excerpts readable: cap at ~300 characters. */
    private String excerpt(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "…";
    }
}
