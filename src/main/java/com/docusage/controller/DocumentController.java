package com.docusage.controller;

import com.docusage.dto.DocumentResponse;
import com.docusage.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Document management: upload (which triggers indexing), list, inspect, delete. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return DocumentResponse.from(documentService.upload(file));
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return documentService.list().stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable String id) {
        return DocumentResponse.from(documentService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
