package com.docusage.dto;

import java.util.List;

/** An answer plus the exact passages it was grounded in. */
public record AskResponse(
        String answer,
        List<Citation> citations,
        String embeddingProvider,
        String chatProvider
) {

    /** One retrieved passage backing the answer. */
    public record Citation(
            String documentId,
            String documentName,
            int chunkIndex,
            double score,
            String excerpt
    ) {
    }
}
