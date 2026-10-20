package com.docusage.controller;

import com.docusage.repository.ChunkRepository;
import com.docusage.repository.DocumentRepository;
import com.docusage.service.VectorIndex;
import com.docusage.service.chat.ChatProvider;
import com.docusage.service.embedding.EmbeddingProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Liveness + a quick view of what the system currently holds and which providers are active. */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final VectorIndex index;
    private final EmbeddingProvider embeddingProvider;
    private final ChatProvider chatProvider;

    public HealthController(DocumentRepository documentRepository,
                            ChunkRepository chunkRepository,
                            VectorIndex index,
                            EmbeddingProvider embeddingProvider,
                            ChatProvider chatProvider) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.index = index;
        this.embeddingProvider = embeddingProvider;
        this.chatProvider = chatProvider;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "documents", documentRepository.count(),
                "chunks", chunkRepository.count(),
                "indexedChunks", index.size(),
                "embeddingProvider", embeddingProvider.name(),
                "chatProvider", chatProvider.name()
        );
    }
}
