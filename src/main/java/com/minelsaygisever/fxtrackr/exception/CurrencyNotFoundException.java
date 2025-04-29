package com.minelsaygisever.fxtrackr.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String currency) {
        super("Unsupported currency code: " + currency);
    }
}
