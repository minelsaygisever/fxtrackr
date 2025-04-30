package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import com.minelsaygisever.fxtrackr.exception.InvalidAmountException;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyConversionService.
 */
@SpringBootTest
class CurrencyConversionServiceTest {

    @MockBean
    private FixerRestClient fixerRestClient;

    @Autowired
    private CurrencyConversionService conversionService;

    @MockBean
    private CurrencyConversionRepository currencyConversionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getExchangeRate - successful conversion and normalization")
    void testGetExchangeRate_Success() {
        // Arrange
        when(fixerRestClient.getRate("USD", "EUR")).thenReturn(new BigDecimal("1.23"));

        // Act
        ExchangeRateResponse response = conversionService.getExchangeRate(" usd ", "eur");

        // Assert
        assertEquals(new BigDecimal("1.23"), response.getExchangeRate());
    }

    @Test
    @DisplayName("getExchangeRate - external API exception is propagated")
    void testGetExchangeRate_ExternalApiException() {
        // Arrange
        when(fixerRestClient.getRate(anyString(), anyString()))
                .thenThrow(new ExternalApiException("External service failure"));

        // Act & Assert
        ExternalApiException ex = assertThrows(
                ExternalApiException.class,
                () -> conversionService.getExchangeRate("USD", "EUR")
        );
        assertEquals("External service failure", ex.getMessage());
    }

    @Test
    @DisplayName("getExchangeRate - currency not found exception is propagated")
    void testGetExchangeRate_CurrencyNotFound() {
        // Arrange
        when(fixerRestClient.getRate("ABC", "EUR"))
                .thenThrow(new CurrencyNotFoundException("Currency not supported: ABC"));

        // Act & Assert
        CurrencyNotFoundException ex = assertThrows(
                CurrencyNotFoundException.class,
                () -> conversionService.getExchangeRate("ABC", "EUR")
        );
        assertTrue(ex.getMessage().contains("ABC"));
    }

    @Test
    @DisplayName("convertAndSaveCurrency – currency not found exception bubbles up")
    void testConvertAndSaveCurrency_CurrencyNotFound() {
        when(fixerRestClient.getRate("ABC","EUR"))
                .thenThrow(new CurrencyNotFoundException("Currency not supported: ABC"));

        CurrencyNotFoundException ex = assertThrows(
                CurrencyNotFoundException.class,
                () -> conversionService.convertAndSaveCurrency(BigDecimal.ONE, "ABC", "EUR")
        );
        assertTrue(ex.getMessage().contains("ABC"));
        verify(currencyConversionRepository, never()).save(any());
    }

    @Test
    @DisplayName("convertAndSaveCurrency – negative amount throws")
    void testConvertAndSaveCurrency_NegativeAmount() {
        BigDecimal negative = new BigDecimal("-5.00");

        InvalidAmountException ex = assertThrows(
                InvalidAmountException.class,
                () -> conversionService.convertAndSaveCurrency(negative, "USD", "EUR")
        );
        assertEquals("Amount must be greater than zero", ex.getMessage());

        verifyNoInteractions(fixerRestClient, currencyConversionRepository);
    }

    @Test
    @DisplayName("getConversionHistory - by transactionId and no date")
    void testGetHistory_ByTransactionId_NoDate() {
        CurrencyConversion entity = CurrencyConversion.builder()
                .id("tx-1")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.000000"))
                .convertedAmount(new BigDecimal("92.340000"))
                .exchangeRate(new BigDecimal("0.923400"))
                .timestamp(Instant.parse("2025-04-30T12:00:00Z"))
                .build();
        when(currencyConversionRepository.findById("tx-1"))
                .thenReturn(Optional.of(entity));

        Page<ConversionHistoryResponse> result = conversionService
                .getConversionHistory("tx-1", null, Pageable.ofSize(10));

        assertEquals(1, result.getTotalElements());
        assertEquals("tx-1", result.getContent().get(0).getTransactionId());
    }

    @Test
    @DisplayName("getConversionHistory - by transactionId and date mismatch")
    void testGetHistory_ByTransactionId_DateMismatch() {
        CurrencyConversion entity = CurrencyConversion.builder()
                .id("tx-2")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("50.000000"))
                .convertedAmount(new BigDecimal("46.170000"))
                .exchangeRate(new BigDecimal("0.923400"))
                .timestamp(Instant.parse("2025-04-29T08:30:00Z"))
                .build();
        when(currencyConversionRepository.findById("tx-2"))
                .thenReturn(Optional.of(entity));

        Page<ConversionHistoryResponse> result = conversionService
                .getConversionHistory("tx-2", LocalDate.of(2025, 4, 30), Pageable.unpaged());

        assertEquals(0, result.getTotalElements(),
                "Expected no results when transaction date does not match filter");
    }

    @Test
    @DisplayName("getConversionHistory - by date range")
    void testGetHistory_ByDateRange() {
        CurrencyConversion e1 = CurrencyConversion.builder()
                .id("tx-3")
                .sourceCurrency("GBP")
                .targetCurrency("USD")
                .sourceAmount(new BigDecimal("200.000000"))
                .convertedAmount(new BigDecimal("260.000000"))
                .exchangeRate(new BigDecimal("1.300000"))
                .timestamp(Instant.parse("2025-04-30T01:00:00Z"))
                .build();
        CurrencyConversion e2 = CurrencyConversion.builder()
                .id("tx-4")
                .sourceCurrency("JPY")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("15000.000000"))
                .convertedAmount(new BigDecimal("100.000000"))
                .exchangeRate(new BigDecimal("0.006667"))
                .timestamp(Instant.parse("2025-04-30T23:00:00Z"))
                .build();

        when(currencyConversionRepository.findByTimestampBetween(
                any(Instant.class),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(e1, e2)));

        Page<ConversionHistoryResponse> page = conversionService
                .getConversionHistory(null, LocalDate.of(2025, 4, 30), Pageable.ofSize(5));

        assertEquals(2, page.getTotalElements());
        assertEquals("tx-3", page.getContent().get(0).getTransactionId());
        assertEquals("tx-4", page.getContent().get(1).getTransactionId());
    }
}
