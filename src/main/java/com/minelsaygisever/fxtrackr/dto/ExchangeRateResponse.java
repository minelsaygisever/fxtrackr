package com.minelsaygisever.fxtrackr.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangeRateResponse {
    private String from;
    private String to;
    private BigDecimal rate;
}
