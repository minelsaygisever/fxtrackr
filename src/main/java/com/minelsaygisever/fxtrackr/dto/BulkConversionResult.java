package com.minelsaygisever.fxtrackr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BulkConversionResult {
    @Schema(description = "Line number in the uploaded CSV", example = "1")
    private int line;

    @Schema(description = "Transaction ID if conversion succeeded", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String transactionId;

    @Schema(description = "Converted amount if succeeded", example = "92.340000")
    private BigDecimal convertedAmount;

    @Schema(description = "Result code, e.g. SUCCESS or specific error code", example = "SUCCESS")
    private String code;

    @Schema(description = "Error message if any", example = "OK")
    private String message;
}