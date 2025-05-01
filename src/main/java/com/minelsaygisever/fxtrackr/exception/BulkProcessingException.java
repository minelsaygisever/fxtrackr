package com.minelsaygisever.fxtrackr.exception;

public class BulkProcessingException extends RuntimeException {
    public BulkProcessingException(String msg, Throwable cause) {
        super(msg, cause);
    }
    public BulkProcessingException(String msg) {
        super(msg);
    }
}
