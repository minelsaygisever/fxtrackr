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

    @Schema(description="Transaction ID if successful, otherwise null", example="fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String transactionId;

    @Schema(description="Converted amount if successful, otherwise null", example="92.34")
    private BigDecimal convertedAmount;

    @Schema(description="One of: SUCCESS, INVALID_AMOUNT, INVALID_CURRENCY, EXTERNAL_API_ERROR, PROCESSING_ERROR",
            example="SUCCESS")
    private String code;

    @Schema(description="Detailed message for this row", example="OK")
    private String message;
}