package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
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

import java.math.BigDecimal;

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
}
