package com.minelsaygisever.fxtrackr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a conversion is requested for a currency
 * that is not supported or not active in the system.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CurrencyNotSupportedException extends RuntimeException {

    public CurrencyNotSupportedException(String currencyCode) {
        super("The currency '" + currencyCode + "' is not supported or is inactive.");
    }
}

