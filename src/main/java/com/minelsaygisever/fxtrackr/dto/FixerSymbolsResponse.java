package com.minelsaygisever.fxtrackr.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FixerSymbolsResponse {
    /**
     * Indicates if the API request was successful.
     */
    private boolean success;

    /**
     * A map where the key is the currency code (e.g., "USD")
     * and the value is the full currency name (e.g., "United States Dollar").
     */
    private Map<String, String> symbols;
    private FixerError error;

}
