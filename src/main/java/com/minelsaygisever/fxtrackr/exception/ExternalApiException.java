package com.minelsaygisever.fxtrackr.exception;


public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) {
        super(message);
    }
    public ExternalApiException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
