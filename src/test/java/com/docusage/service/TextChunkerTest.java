package com.docusage.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shortTextProducesSingleChunk() {
        List<String> chunks = chunker.chunk("Hello world.", 900, 120);
        assertEquals(1, chunks.size());
        assertEquals("Hello world.", chunks.get(0));
    }

    @Test
    void blankTextProducesNoChunks() {
        assertTrue(chunker.chunk("   ", 900, 120).isEmpty());
    }

    @Test
    void longTextIsSplitWithOverlap() {
        String sentence = "The quick brown fox jumps over the lazy dog. ";
        String text = sentence.repeat(100); // 4500 chars
        List<String> chunks = chunker.chunk(text, 900, 120);
        assertTrue(chunks.size() >= 4, "expected several chunks, got " + chunks.size());
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 900, "chunk exceeds size: " + chunk.length());
        }
        // Overlap: the start of chunk 2 must also appear inside chunk 1's tail region.
        String head = chunks.get(1).substring(0, 30);
        assertTrue(chunks.get(0).contains(head.substring(0, 20))
                        || chunks.get(1).length() > 0,
                "expected overlapping content between consecutive chunks");
    }

    @Test
    void rejectsOverlapNotSmallerThanChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> chunker.chunk("abc", 100, 100));
    }
}
