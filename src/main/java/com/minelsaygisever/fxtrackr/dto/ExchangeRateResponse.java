package com.minelsaygisever.fxtrackr.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangeRateResponse {
    @Schema(description = "Current exchange rate between source and target currency", example = "0.918273")
    private BigDecimal exchangeRate;
}
