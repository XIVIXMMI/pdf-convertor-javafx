package com.lazydev.pdf_convert.exception;

public class PDFProcessingException extends RuntimeException {

    public PDFProcessingException(String message) {
        super(message);
    }

    public PDFProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PDFProcessingException(Throwable cause) {
        super(cause);
    }
}