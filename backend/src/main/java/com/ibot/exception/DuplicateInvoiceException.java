package com.ibot.exception;

public class DuplicateInvoiceException extends RuntimeException {
    private final Long existingInvoiceId;

    public DuplicateInvoiceException(String message, Long existingInvoiceId) {
        super(message);
        this.existingInvoiceId = existingInvoiceId;
    }

    public Long getExistingInvoiceId() {
        return existingInvoiceId;
    }
}
