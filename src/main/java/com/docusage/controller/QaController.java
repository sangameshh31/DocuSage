package com.docusage.controller;

import com.docusage.dto.AskRequest;
import com.docusage.dto.AskResponse;
import com.docusage.service.QaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Q&A endpoint: ask across all documents, or scope to one with documentId. */
@RestController
@RequestMapping("/api")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return qaService.ask(request.question(), request.documentId());
    }
}
