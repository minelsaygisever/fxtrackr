package com.minelsaygisever.fxtrackr.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class FixerResponse {
    private boolean success;
    private Map<String, BigDecimal> rates;
    private FixerError error;
}

