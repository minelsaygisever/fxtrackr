package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.FilterParameterException;
import com.minelsaygisever.fxtrackr.exception.InvalidAmountException;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
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

    public Page<ConversionHistoryResponse> getConversionHistory(
            String transactionId,
            LocalDate date,
            Pageable pageable
    ) {
        // 1) If transactionId is present, always look it up first
        if (transactionId != null && !transactionId.isBlank()) {
            return currencyConversionRepository.findById(transactionId)
                    .map(entity -> {
                        // If a date filter was also provided, verify the entity’s timestamp matches
                        if (date != null) {
                            LocalDate entityDate =
                                    entity.getTimestamp()
                                            .atZone(ZoneOffset.UTC)
                                            .toLocalDate();
                            // If it doesn’t match, return an empty page
                            if (!entityDate.equals(date)) {
                                return new PageImpl<ConversionHistoryResponse>(
                                        Collections.emptyList(),
                                        pageable,
                                        0L
                                );
                            }
                        }
                        // No date filter or date matches — return a single‐item page
                        return new PageImpl<>(
                                Collections.singletonList(toResponse(entity)),
                                pageable,
                                1L
                        );
                    })
                    // If the ID isn’t found, return an empty page
                    .orElseGet(() -> new PageImpl<ConversionHistoryResponse>(
                            Collections.emptyList(),
                            pageable,
                            0L
                    ));
        }

        // 2) If only a date filter is provided, perform a normal date‐range query
        if (date != null) {
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end   = date.plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

            return currencyConversionRepository
                    .findByTimestampBetween(start, end, pageable)
                    .map(this::toResponse);
        }

        throw new FilterParameterException("Either transactionId or date must be provided");
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

    private ConversionHistoryResponse toResponse(CurrencyConversion e) {
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
