package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CurrencyConversionService.
 */
class CurrencyConversionServiceTest {

    @Mock
    private FixerRestClient fixerRestClient;

    @InjectMocks
    private CurrencyConversionService conversionService;

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
        assertEquals("USD", response.getFrom());
        assertEquals("EUR", response.getTo());
        assertEquals(new BigDecimal("1.23"), response.getRate());
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
}
