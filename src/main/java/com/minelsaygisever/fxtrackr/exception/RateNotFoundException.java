package com.minelsaygisever.fxtrackr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class RateNotFoundException extends ApplicationException {
    private static final String ERROR_CODE = "RATE_NOT_AVAILABLE";

    public RateNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
