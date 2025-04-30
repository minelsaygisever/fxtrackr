package com.minelsaygisever.fxtrackr.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CurrencyConversionResponse {
    @Schema(description = "Unique transaction identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String transactionId;

    @Schema(description = "Converted amount in the target currency", example = "92.34")
    private BigDecimal convertedAmount;
}
