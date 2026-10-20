package com.docusage.dto;

import jakarta.validation.constraints.NotBlank;

/** Ask a question. If documentId is given, only that document is searched. */
public record AskRequest(
        @NotBlank(message = "question must not be blank") String question,
        String documentId
) {
}
