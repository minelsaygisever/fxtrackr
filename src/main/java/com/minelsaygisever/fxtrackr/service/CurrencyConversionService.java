package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final FixerRestClient fixerRestClient;

    public ExchangeRateResponse getExchangeRate(String from, String to) {
        String fromNorm = from.trim().toUpperCase(Locale.ROOT);
        String toNorm   = to.trim().toUpperCase(Locale.ROOT);

        BigDecimal rate = fixerRestClient.getRate(fromNorm, toNorm);
        return ExchangeRateResponse.builder()
                .from(fromNorm)
                .to(toNorm)
                .rate(rate)
                .build();
    }
}
