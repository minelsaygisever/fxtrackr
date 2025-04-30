package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        .exchangeRate(BigDecimal.valueOf(1.23))
                        .build());

        // Act & Assert
        mockMvc.perform(get("/api/exchange-rate")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value(1.23));    // artık sadece rate bakıyoruz
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

    @Test
    @DisplayName("POST /api/convert - Success")
    void testConvertCurrency_Success() throws Exception {
        // Arrange
        CurrencyConversionResponse response = CurrencyConversionResponse.builder()
                .transactionId("tx-123")
                .convertedAmount(BigDecimal.valueOf(92.34))
                .build();

        when(currencyConversionService.convertAndSaveCurrency(
                BigDecimal.valueOf(10000, 2), "USD", "EUR"))
                .thenReturn(response);

        String requestJson = "{ \"amount\": 100.00, \"from\": \"USD\", \"to\": \"EUR\" }";

        // Act & Assert
        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-123"))
                .andExpect(jsonPath("$.convertedAmount").value(92.34));
    }

    @Test
    @DisplayName("POST /api/convert - Invalid Currency")
    void testConvertCurrency_InvalidCurrency() throws Exception {
        // Invalid 'from' field (only 2 letters)
        String requestJson = "{ \"amount\": 100.00, \"from\": \"US\", \"to\": \"EUR\" }";

        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/convert - Invalid Body")
    void testConvertCurrency_InvalidBody() throws Exception {
        // Missing 'from' field (null or blank)
        String requestJson = "{ \"amount\": 100.00, \"to\": \"EUR\" }";

        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/convert - External API Failure")
    void testConvertCurrency_ExternalApiFailure() throws Exception {
        when(currencyConversionService.convertAndSaveCurrency(
                BigDecimal.valueOf(10000, 2), "USD", "EUR"
        )).thenThrow(new ExternalApiException("Fixer API failed"));

        String requestJson = "{ \"amount\": 100.00, \"from\": \"USD\", \"to\": \"EUR\" }";

        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"))
                .andExpect(jsonPath("$.message").value("Fixer API failed"));
    }

}