package com.docusage.exception;

/** Thrown when a requested resource (e.g. a document id) does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
