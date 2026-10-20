package com.docusage.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits raw document text into overlapping chunks.
 *
 * <p>Why chunk at all? An LLM prompt (and a useful citation) needs a small,
 * focused passage, not a 200-page document. Fixed-size windows with overlap
 * are the standard first approach: the overlap means a sentence that straddles
 * a boundary still appears whole in at least one chunk. The splitter prefers
 * to end a chunk at a paragraph or sentence boundary near the size limit so
 * chunks read naturally.
 */
@Component
public class TextChunker {

    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be >= 0 and < chunkSize");
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        List<String> chunks = new ArrayList<>();
        if (normalized.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            if (end < normalized.length()) {
                end = findBoundary(normalized, start, end);
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            // Step forward, keeping `overlap` characters of context.
            start = Math.max(end - overlap, start + 1);
            // Don't start the next chunk mid-word if we can avoid it.
            int space = normalized.indexOf(' ', start);
            if (space > start && space - start < 40) {
                start = space + 1;
            }
        }
        return chunks;
    }

    /** Look backwards from the hard limit for a paragraph/sentence/word boundary. */
    private int findBoundary(String text, int start, int hardEnd) {
        int searchFrom = Math.max(start + 1, hardEnd - 200);
        int paragraph = text.lastIndexOf(". ", hardEnd);
        if (paragraph >= searchFrom) {
            return paragraph + 1; // keep the full stop with the chunk
        }
        int space = text.lastIndexOf(' ', hardEnd);
        if (space >= searchFrom) {
            return space;
        }
        return hardEnd;
    }
}
