package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import com.minelsaygisever.fxtrackr.exception.GlobalExceptionHandler;
import com.minelsaygisever.fxtrackr.service.CurrencyConversionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for CurrencyConversionController - only the exchange-rate endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class CurrencyConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyConversionService currencyConversionService;

    @Test
    @DisplayName("GET /api/exchange-rate - Success")
    void testGetExchangeRate_Success() throws Exception {
        // Arrange
        when(currencyConversionService.getExchangeRate("USD", "EUR"))
                .thenReturn(ExchangeRateResponse.builder()
                        .from("USD")
                        .to("EUR")
                        .rate(BigDecimal.valueOf(1.23))
                        .build());

        // Act & Assert
        mockMvc.perform(get("/api/exchange-rate")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.rate").value(1.23));
    }

    @Test
    @DisplayName("GET /api/exchange-rate - Invalid Parameter Format")
    void testGetExchangeRate_InvalidParam() throws Exception {
        // Missing or invalid 'from' param (only 2 letters)
        mockMvc.perform(get("/api/exchange-rate")
                        .param("from", "US")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/exchange-rate - External API Error")
    void testGetExchangeRate_ExternalApiError() throws Exception {
        // Arrange
        when(currencyConversionService.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalApiException("Failed to call Fixer API"));

        // Act & Assert
        mockMvc.perform(get("/api/exchange-rate")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"));
    }
}