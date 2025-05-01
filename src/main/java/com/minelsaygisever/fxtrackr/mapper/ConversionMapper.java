package com.minelsaygisever.fxtrackr.mapper;

import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import org.springframework.stereotype.Component;

@Component
public class ConversionMapper {
    public ConversionHistoryResponse toHistoryResponse(CurrencyConversion e) {
        return ConversionHistoryResponse.builder()
                .transactionId(e.getId())
                .sourceCurrency(e.getSourceCurrency())
                .targetCurrency(e.getTargetCurrency())
                .sourceAmount(e.getSourceAmount())
                .convertedAmount(e.getConvertedAmount())
                .exchangeRate(e.getExchangeRate())
                .timestamp(e.getTimestamp())
                .build();
    }
}
