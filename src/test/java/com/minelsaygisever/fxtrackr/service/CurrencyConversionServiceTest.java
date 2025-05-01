package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.BulkConversionResult;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import com.minelsaygisever.fxtrackr.exception.InvalidAmountException;
import com.minelsaygisever.fxtrackr.exception.InvalidCsvHeaderException;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import com.minelsaygisever.fxtrackr.validation.ValidationUtil;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    ValidationUtil validationUtil;

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
        when(fixerRestClient.getRate("USD", "EUR")).thenReturn(new BigDecimal("1.23"));

        ExchangeRateResponse response = conversionService.getExchangeRate(" uSd ", "euR ");

        assertEquals(new BigDecimal("1.23"), response.getExchangeRate());
    }

    @Test
    @DisplayName("getExchangeRate - external API exception is propagated")
    void testGetExchangeRate_ExternalApiException() {
        when(fixerRestClient.getRate(anyString(), anyString()))
                .thenThrow(new ExternalApiException("External service failure"));

        ExternalApiException ex = assertThrows(
                ExternalApiException.class,
                () -> conversionService.getExchangeRate("USD", "EUR")
        );
        assertEquals("External service failure", ex.getMessage());
    }

    @Test
    @DisplayName("getExchangeRate - currency not found exception is propagated")
    void testGetExchangeRate_CurrencyNotFound() {
        when(fixerRestClient.getRate("ABC", "EUR"))
                .thenThrow(new CurrencyNotFoundException("Currency not supported: ABC"));

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
                .sourceCurrency("TRY")
                .targetCurrency("USD")
                .sourceAmount(new BigDecimal("200.000000"))
                .convertedAmount(new BigDecimal("260.000000"))
                .exchangeRate(new BigDecimal("1.300000"))
                .timestamp(Instant.parse("2025-04-30T01:00:00Z"))
                .build();
        CurrencyConversion e2 = CurrencyConversion.builder()
                .id("tx-4")
                .sourceCurrency("USD")
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

    @Test
    @DisplayName("bulkConvert - invalid CSV header throws InvalidCsvHeaderException")
    void testBulkConvert_InvalidHeader() throws Exception {
        String csv =
                "amount,from\n" +
                "100.00,USD";

        MockMultipartFile file = new MockMultipartFile(
                "file","bad.csv","text/csv", csv.getBytes()
        );

        InvalidCsvHeaderException ex = assertThrows(
                InvalidCsvHeaderException.class,
                () -> conversionService.bulkConvert(file)
        );
        assertTrue(ex.getMessage().contains("missing columns [to]"));
    }

    @Test
    @DisplayName("bulkConvert - mixed valid and invalid rows")
    void testBulkConvert_MixedRows() throws Exception {
        // Stub the external rate
        when(fixerRestClient.getRate("USD", "EUR")).thenReturn(new BigDecimal("1.23"));

        when(currencyConversionRepository.save(any()))
                .thenAnswer(invocation -> {
                    CurrencyConversion e = invocation.getArgument(0);
                    e.setId(UUID.randomUUID().toString());
                    return e;
                });

        String csv =
                "amount,from,to\n" +
                "100.00,USD,EUR\n" +
                "-5.00,USD,EUR\n" + // INVALID_AMOUNT
                "50.50,USD,EUR";
        MockMultipartFile file = new MockMultipartFile(
                "file","mix.csv","text/csv", csv.getBytes()
        );

        List<BulkConversionResult> results = conversionService.bulkConvert(file);

        assertEquals(3, results.size());

        assertEquals("SUCCESS",         results.get(0).getCode());
        assertEquals("INVALID_AMOUNT",  results.get(1).getCode());
        assertEquals("SUCCESS",         results.get(2).getCode());
    }

    @Test
    @DisplayName("bulkConvert - processing error does not stop other rows")
    void testBulkConvert_RowLevelErrorHandling() throws Exception {
        when(fixerRestClient.getRate("USD","EUR"))
                .thenReturn(new BigDecimal("1.00"))
                .thenThrow(new ExternalApiException("Fixer down"))
                .thenReturn(new BigDecimal("1.00"));
        when(currencyConversionRepository.save(any()))
                .thenAnswer(invocation -> {
                    CurrencyConversion e = invocation.getArgument(0);
                    e.setId(UUID.randomUUID().toString());
                    return e;
                });


        String csv =
                "amount,from,to\n" +
                "10.00,USD,EUR\n" +    // row 1: SUCCESS
                "20.00,USD,EUR\n" +    // row 2: EXTERNAL_API_ERROR
                "30.00,USD,EUR\n";     // row 3: SUCCESS

        MockMultipartFile file = new MockMultipartFile("file","test.csv","text/csv", csv.getBytes());

        List<BulkConversionResult> results = conversionService.bulkConvert(file);

        // Verify that save(...) is only invoked for the two SUCCESS rows, and not for the failed one
        verify(currencyConversionRepository, times(2)).save(any());

        assertEquals(3, results.size());

        assertEquals("SUCCESS", results.get(0).getCode());
        assertNotNull(results.get(0).getTransactionId());

        assertEquals("EXTERNAL_API_ERROR", results.get(1).getCode());
        assertNull(results.get(1).getTransactionId()); // ID should be null

        assertEquals("SUCCESS", results.get(2).getCode());
        assertNotNull(results.get(2).getTransactionId());
    }

    @Test
    @DisplayName("bulkConvert - all rows valid returns SUCCESS list")
    void testBulkConvert_AllValidRows() throws Exception {
        // Stub dependencies: fixed rate and repository save
        when(fixerRestClient.getRate(anyString(), anyString()))
                .thenReturn(new BigDecimal("2.0"));
        when(currencyConversionRepository.save(any(CurrencyConversion.class)))
                .thenAnswer(invocation -> {
                    CurrencyConversion e = invocation.getArgument(0);
                    e.setId(UUID.randomUUID().toString());
                    return e;
                });

        String csv =
                "amount,from,to\n" +
                "100.00,USD,EUR\n" +
                "50.50,USD,EUR";
        MockMultipartFile file = new MockMultipartFile(
                "file","valid.csv","text/csv", csv.getBytes()
        );

        List<BulkConversionResult> results = conversionService.bulkConvert(file);

        // Ensure we persist exactly one record per valid row
        verify(currencyConversionRepository, times(2)).save(any(CurrencyConversion.class));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "SUCCESS".equals(r.getCode())));
        assertEquals(2, results.stream().map(BulkConversionResult::getTransactionId).filter(Objects::nonNull).count());
    }
}
