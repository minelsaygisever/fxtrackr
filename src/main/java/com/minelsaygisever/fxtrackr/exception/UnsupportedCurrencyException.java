package com.minelsaygisever.fxtrackr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a conversion is requested for a currency
 * that is not supported or not active in the system.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedCurrencyException extends ApplicationException {

    private static final String ERROR_CODE = "UNSUPPORTED_CURRENCY";

    public UnsupportedCurrencyException(String currencyCode) {
        super(ERROR_CODE, "The currency '" + currencyCode + "' is not supported or is inactive.");
    }
}

