package com.minelsaygisever.fxtrackr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConversionHistoryResponse {
    @Schema(description = "Unique transaction identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String transactionId;

    @Schema(description = "Source currency code", example = "USD")
    private String sourceCurrency;

    @Schema(description = "Target currency code", example = "EUR")
    private String targetCurrency;

    @Schema(description = "Original amount", example = "100.000000")
    private BigDecimal sourceAmount;

    @Schema(description = "Converted amount", example = "92.340000")
    private BigDecimal convertedAmount;

    @Schema(description = "Exchange rate used", example = "0.923400")
    private BigDecimal exchangeRate;

    @Schema(description = "Timestamp of conversion", example = "2025-04-30T15:04:05Z")
    private Instant timestamp;
}