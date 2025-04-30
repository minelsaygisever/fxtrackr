package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.InvalidAmountException;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final FixerRestClient fixerRestClient;
    private final CurrencyConversionRepository currencyConversionRepository;

    public ExchangeRateResponse getExchangeRate(String from, String to) {
        String fromNorm = normalizeCode(from);
        String toNorm = normalizeCode(to);

        BigDecimal rate = fixerRestClient.getRate(fromNorm, toNorm);
        return ExchangeRateResponse.builder()
                .exchangeRate(rate)
                .build();
    }

    @Transactional
    public CurrencyConversionResponse convertAndSaveCurrency(BigDecimal amount, String from, String to) {
        BigDecimal amountNorm = normalizeAmount(amount);
        String fromNorm = normalizeCode(from);
        String toNorm = normalizeCode(to);

        BigDecimal rate = fixerRestClient.getRate(fromNorm, toNorm);
        BigDecimal convertedAmount = amountNorm.multiply(rate).setScale(6, RoundingMode.HALF_UP);


        CurrencyConversion entity = CurrencyConversion.builder()
                .sourceCurrency(fromNorm)
                .targetCurrency(toNorm)
                .sourceAmount(amountNorm)
                .convertedAmount(convertedAmount)
                .exchangeRate(rate)
                .build();

        CurrencyConversion saved = currencyConversionRepository.save(entity);

        return CurrencyConversionResponse.builder()
                .transactionId(saved.getId())
                .convertedAmount(saved.getConvertedAmount())
                .build();
    }

    /**
     * Normalizes a currency code by trimming whitespace and converting to uppercase
     * using the ROOT locale. Returns null if the input is null.
     */
    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new CurrencyNotFoundException("Currency code is required");
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new CurrencyNotFoundException(normalized);
        }

        return normalized;
    }

    /**
     * Validates and normalizes a BigDecimal amount:
     * - Must not be null
     * - Must be greater than zero
     * - Max 13 integer digits and 6 decimal digits
     * Returns amount scaled to 6 decimal places using HALF_UP.
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (amount.precision() - amount.scale() > 13) {
            throw new InvalidAmountException("Amount can have up to 13 integer digits");
        }
        return amount.setScale(6, RoundingMode.HALF_UP);
    }
}
