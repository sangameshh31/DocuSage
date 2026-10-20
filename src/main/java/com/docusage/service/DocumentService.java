package com.docusage.service;

import com.docusage.config.AppProperties;
import com.docusage.exception.NotFoundException;
import com.docusage.model.ChunkEntity;
import com.docusage.model.DocumentEntity;
import com.docusage.repository.ChunkRepository;
import com.docusage.repository.DocumentRepository;
import com.docusage.service.embedding.EmbeddingProvider;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The ingest pipeline: file → text → chunks → embeddings → database + index.
 * Also rebuilds the in-memory {@link VectorIndex} from the database at
 * startup, so restarts don't lose searchable documents.
 */
@Service
public class DocumentService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final TextChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorIndex index;
    private final AppProperties properties;

    public DocumentService(DocumentRepository documentRepository,
                           ChunkRepository chunkRepository,
                           TextChunker chunker,
                           EmbeddingProvider embeddingProvider,
                           VectorIndex index,
                           AppProperties properties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.index = index;
        this.properties = properties;
    }

    /** Rebuild the in-memory index from persisted chunks on boot. */
    @Override
    public void run(ApplicationArguments args) {
        List<ChunkEntity> all = chunkRepository.findAll();
        for (ChunkEntity chunk : all) {
            index.add(toIndexed(chunk));
        }
        log.info("Vector index rebuilt: {} chunks from {} documents",
                all.size(), documentRepository.count());
    }

    @Transactional
    public DocumentEntity upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String text = extractText(file, filename);
        if (text.isBlank()) {
            throw new IllegalArgumentException("No readable text found in " + filename);
        }

        List<String> pieces = chunker.chunk(text, properties.getChunkSize(), properties.getChunkOverlap());
        String documentId = UUID.randomUUID().toString();

        DocumentEntity document = new DocumentEntity(
                documentId, filename, file.getContentType(), file.getSize(),
                pieces.size(), Instant.now());
        documentRepository.save(document);

        for (int i = 0; i < pieces.size(); i++) {
            String piece = pieces.get(i);
            float[] vector = embeddingProvider.embed(piece);
            ChunkEntity chunk = new ChunkEntity(
                    documentId + ":" + i, documentId, i, piece, VectorIndex.serialize(vector));
            chunkRepository.save(chunk);
            index.add(new VectorIndex.IndexedChunk(chunk.getId(), documentId, i, piece, vector));
        }
        log.info("Indexed document {} ({} chunks) with provider {}",
                filename, pieces.size(), embeddingProvider.name());
        return document;
    }

    public List<DocumentEntity> list() {
        return documentRepository.findAll();
    }

    public DocumentEntity get(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional
    public void delete(String id) {
        get(id); // 404 if missing
        chunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
        index.removeByDocument(id);
    }

    private VectorIndex.IndexedChunk toIndexed(ChunkEntity chunk) {
        return new VectorIndex.IndexedChunk(
                chunk.getId(), chunk.getDocumentId(), chunk.getChunkIndex(),
                chunk.getText(), VectorIndex.deserialize(chunk.getEmbedding()));
    }

    /** Text extraction by file type: PDF via PDFBox, everything else as plain text. */
    private String extractText(MultipartFile file, String filename) throws IOException {
        String lower = filename.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (lower.endsWith(".pdf") || contentType.equals("application/pdf")) {
            try (PDDocument pdf = Loader.loadPDF(file.getBytes())) {
                return new PDFTextStripper().getText(pdf);
            }
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
