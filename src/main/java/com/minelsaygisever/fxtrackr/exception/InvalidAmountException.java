package com.minelsaygisever.fxtrackr.exception;


public class InvalidAmountException extends ApplicationException {
    private static final String ERROR_CODE = "INVALID_AMOUNT";
    public InvalidAmountException(String message) {
        super(ERROR_CODE, message);
    }
}
