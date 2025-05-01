package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.dto.BulkConversionResult;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import com.minelsaygisever.fxtrackr.exception.GlobalExceptionHandler;
import com.minelsaygisever.fxtrackr.exception.InvalidCsvHeaderException;
import com.minelsaygisever.fxtrackr.service.CurrencyConversionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        when(currencyConversionService.getExchangeRate("USD", "EUR"))
                .thenReturn(ExchangeRateResponse.builder()
                        .exchangeRate(BigDecimal.valueOf(1.23))
                        .build());

        mockMvc.perform(get("/api/exchange-rate")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value(1.23));
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
        when(currencyConversionService.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalApiException("Failed to call Fixer API"));

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
        CurrencyConversionResponse response = CurrencyConversionResponse.builder()
                .transactionId("tx-123")
                .convertedAmount(BigDecimal.valueOf(92.34))
                .build();

        when(currencyConversionService.convertAndSaveCurrency(
                BigDecimal.valueOf(10000, 2), "USD", "EUR"))
                .thenReturn(response);

        String requestJson = "{ \"amount\": 100.00, \"from\": \"USD\", \"to\": \"EUR\" }";

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

    @Test
    @DisplayName("POST /api/conversions/search - by transactionId found")
    void testSearchHistory_ByTransactionId_Found() throws Exception {
        ConversionHistoryResponse dto = ConversionHistoryResponse.builder()
                .transactionId("tx-123")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.000000"))
                .convertedAmount(new BigDecimal("92.340000"))
                .exchangeRate(new BigDecimal("0.923400"))
                .timestamp(Instant.parse("2025-04-30T12:00:00Z"))
                .build();
        Page<ConversionHistoryResponse> page = new PageImpl<>(List.of(dto));
        when(currencyConversionService.getConversionHistory(
                eq("tx-123"),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        String requestJson = "{ \"transactionId\": \"tx-123\" }";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-123"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/conversions/search - by transactionId not found")
    void testSearchHistory_ByTransactionId_NotFound() throws Exception {
        when(currencyConversionService.getConversionHistory(
                eq("nope"),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Page.empty());

        String requestJson = "{ \"transactionId\": \"nope\" }";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("POST /api/conversions/search - by date range")
    void testSearchHistory_ByDate_Found() throws Exception {
        ConversionHistoryResponse dto = ConversionHistoryResponse.builder()
                .transactionId("tx-456")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.000000"))
                .convertedAmount(new BigDecimal("92.340000"))
                .exchangeRate(new BigDecimal("0.923400"))
                .timestamp(Instant.parse("2025-04-30T12:00:00Z"))
                .build();
        Page<ConversionHistoryResponse> page = new PageImpl<>(List.of(dto));

        LocalDate filterDate = LocalDate.of(2025, 4, 30);
        when(currencyConversionService.getConversionHistory(
                isNull(),
                eq(filterDate),
                any(Pageable.class)))
                .thenReturn(page);

        String requestJson = "{ \"date\": \"2025-04-30\" }";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-456"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/conversions/search - by transactionId and date match")
    void testSearchHistory_ByTransactionIdAndDate_Match() throws Exception {
        ConversionHistoryResponse dto = ConversionHistoryResponse.builder()
                .transactionId("tx-123")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.000000"))
                .convertedAmount(new BigDecimal("92.340000"))
                .exchangeRate(new BigDecimal("0.923400"))
                .timestamp(Instant.parse("2025-04-30T12:00:00Z"))
                .build();
        Page<ConversionHistoryResponse> page = new PageImpl<>(List.of(dto));
        LocalDate filterDate = LocalDate.of(2025, 4, 30);

        when(currencyConversionService.getConversionHistory(
                eq("tx-123"),
                eq(filterDate),
                any(Pageable.class)))
                .thenReturn(page);

        String requestJson = "{\n" +
                "  \"transactionId\": \"tx-123\",\n" +
                "  \"date\": \"2025-04-30\"\n" +
                "}";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-123"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/conversions/search - invalid date format")
    void testSearchHistory_InvalidDateFormat() throws Exception {
        String requestJson = "{\n" +
                "  \"date\": \"30-04-2025\"\n" +
                "}";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/conversions/search - missing filters => 400")
    void testSearchHistory_MissingFilters() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(post("/api/conversions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_FORMAT"));
    }

    @Test
    @DisplayName("POST /api/convert/bulk - invalid CSV header → 400")
    void testBulkConvert_InvalidHeader() throws Exception {
        when(currencyConversionService.bulkConvert(any()))
                .thenThrow(new InvalidCsvHeaderException("Invalid CSV header: missing columns [to]"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.csv", "text/csv",
                ("amount,from\n" +
                        "100.00,USD\n").getBytes()
        );

        mockMvc.perform(multipart("/api/convert/bulk").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CSV_HEADER"))
                .andExpect(jsonPath("$.message").value("Invalid CSV header: missing columns [to]"));
    }

    @Test
    @DisplayName("POST /api/convert/bulk - mixed rows → returns per-row codes")
    void testBulkConvert_MixedRows() throws Exception {
        // prepare two row results
        BulkConversionResult r1 = BulkConversionResult.builder()
                .line(1)
                .transactionId("tx-1")
                .convertedAmount(new BigDecimal("90.00"))
                .code("SUCCESS")
                .message("OK")
                .build();
        BulkConversionResult r2 = BulkConversionResult.builder()
                .line(2)
                .code("INVALID_AMOUNT")
                .message("Amount must be greater than zero")
                .build();

        when(currencyConversionService.bulkConvert(any()))
                .thenReturn(List.of(r1, r2));

        String csv =
                "amount,from,to\n" +
                        "100.00,USD,EUR\n" +
                        "-5.00,USD,EUR\n"; // INVALID_AMOUNT
        MockMultipartFile file = new MockMultipartFile(
                "file","mix.csv","text/csv", csv.getBytes()
        );

        mockMvc.perform(multipart("/api/convert/bulk").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].line").value(1))
                .andExpect(jsonPath("$[0].code").value("SUCCESS"))
                .andExpect(jsonPath("$[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$[1].line").value(2))
                .andExpect(jsonPath("$[1].code").value("INVALID_AMOUNT"))
                .andExpect(jsonPath("$[1].message").value("Amount must be greater than zero"));
    }

}