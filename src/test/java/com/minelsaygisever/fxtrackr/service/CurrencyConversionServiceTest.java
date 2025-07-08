package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.Currency;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.BulkConversionResult;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.*;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import com.minelsaygisever.fxtrackr.repository.CurrencyRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyConversionService, updated for caching and new architecture.
 */
@SpringBootTest
class CurrencyConversionServiceTest {

    @MockBean
    private ExchangeRateCacheService exchangeRateCacheService;

    @MockBean
    private CurrencyConversionRepository currencyConversionRepository;

    @MockBean
    private CurrencyRepository currencyRepository;

    @MockBean
    private FixerRestClient fixerRestClient;
    @Autowired
    private CurrencyConversionService conversionService;

    private final Map<Object, Object> mockRates = Map.of(
            "USD", new BigDecimal("1.1"),
            "EUR", new BigDecimal("1.0"),
            "GBP", new BigDecimal("0.9")
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // assume all specified currencies are valid
        when(currencyRepository.findByCodeAndIsActiveTrue(anyString()))
                .thenAnswer(invocation -> {
                    String code = invocation.getArgument(0);
                    if ("USD".equals(code) || "EUR".equals(code) || "GBP".equals(code) || "TRY".equals(code)) {
                        return Optional.of(new Currency(code, "A valid currency", true));
                    }
                    return Optional.empty(); // By default, currency is not found
                });
    }

    @Test
    @DisplayName("getExchangeRate - success with cached rates")
    void testGetExchangeRate_Success() {
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.of(mockRates));

        ExchangeRateResponse response = conversionService.getExchangeRate(" uSd ", "gBp ");

        BigDecimal expectedRate = new BigDecimal("0.818182");
        assertEquals(0, expectedRate.compareTo(response.getExchangeRate()));
    }

    @Test
    @DisplayName("getExchangeRate - cache miss, success with API fallback")
    void testGetExchangeRate_CacheMiss_ApiSuccess() {
        Map<String, BigDecimal> liveRates = Map.of(
                "USD", new BigDecimal("1.2"),
                "EUR", new BigDecimal("1.0"),
                "GBP", new BigDecimal("0.85")
        );
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.empty()); // Cache miss
        when(fixerRestClient.getLatestRates()).thenReturn(liveRates); // API call

        ExchangeRateResponse response = conversionService.getExchangeRate("USD", "GBP");

        verify(exchangeRateCacheService, times(1)).updateRates(liveRates);

        BigDecimal expectedRate = new BigDecimal("0.708333");
        assertEquals(0, expectedRate.compareTo(response.getExchangeRate()));
    }

    @Test
    @DisplayName("getExchangeRate - unsupported currency throws UnsupportedCurrencyException")
    void testGetExchangeRate_UnsupportedCurrency() {
        when(currencyRepository.findByCodeAndIsActiveTrue("XXX")).thenReturn(Optional.empty());

        UnsupportedCurrencyException ex = assertThrows(
                UnsupportedCurrencyException.class,
                () -> conversionService.getExchangeRate("USD", "XXX")
        );
        assertTrue(ex.getMessage().contains("'XXX' is not supported"));
    }

    @Test
    @DisplayName("getExchangeRate - rate not in data source throws RateNotFoundException")
    void testGetExchangeRate_RateNotFoundInMap() {
        Map<Object, Object> partialRates = Map.of("USD", new BigDecimal("1.1")); // GBP is missing
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.of(partialRates));

        RateNotFoundException ex = assertThrows(
                RateNotFoundException.class,
                () -> conversionService.getExchangeRate("USD", "GBP")
        );
        assertTrue(ex.getMessage().contains("Rate for USD or GBP not found"));
    }

    @Test
    @DisplayName("convertAndSaveCurrency - success")
    void testConvertAndSaveCurrency_Success() {
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.of(mockRates));
        when(currencyConversionRepository.save(any(CurrencyConversion.class)))
                .thenAnswer(inv -> {
                    CurrencyConversion conversion = inv.getArgument(0);
                    conversion.setId(UUID.randomUUID().toString());
                    return conversion;
                });

        var response = conversionService.convertAndSaveCurrency(BigDecimal.TEN, "USD", "GBP");

        assertNotNull(response.getTransactionId());
        assertEquals(0, new BigDecimal("8.181820").compareTo(response.getConvertedAmount()));

        verify(currencyConversionRepository, times(1)).save(any(CurrencyConversion.class));
    }

    @Test
    @DisplayName("convertAndSaveCurrency – negative amount throws InvalidAmountException")
    void testConvertAndSaveCurrency_NegativeAmount() {
        BigDecimal negative = new BigDecimal("-5.00");

        InvalidAmountException ex = assertThrows(
                InvalidAmountException.class,
                () -> conversionService.convertAndSaveCurrency(negative, "USD", "EUR")
        );
        assertEquals("Amount must be greater than zero", ex.getMessage());
        assertEquals("INVALID_AMOUNT", ex.getErrorCode());
        verifyNoInteractions(exchangeRateCacheService, currencyConversionRepository);
    }

    @Test
    @DisplayName("getConversionHistory - by transactionId")
    void testGetHistory_ByTransactionId_Success() {
        CurrencyConversion entity = CurrencyConversion.builder().id("tx-1").timestamp(Instant.now()).sourceCurrency("USD").targetCurrency("EUR").build();
        when(currencyConversionRepository.findById("tx-1")).thenReturn(Optional.of(entity));

        Page<ConversionHistoryResponse> result = conversionService.getConversionHistory("tx-1", null, Pageable.ofSize(10));

        assertEquals(1, result.getTotalElements());
        assertEquals("tx-1", result.getContent().get(0).getTransactionId());
    }

    @Test
    @DisplayName("getConversionHistory - by date range")
    void testGetHistory_ByDateRange() {
        CurrencyConversion e1 = CurrencyConversion.builder().id("tx-3").timestamp(Instant.parse("2025-04-30T01:00:00Z")).build();
        CurrencyConversion e2 = CurrencyConversion.builder().id("tx-4").timestamp(Instant.parse("2025-04-30T23:00:00Z")).build();
        when(currencyConversionRepository.findByTimestampBetween(any(), any(), any())).thenReturn(new PageImpl<>(List.of(e1, e2)));

        Page<ConversionHistoryResponse> page = conversionService.getConversionHistory(null, LocalDate.of(2025, 4, 30), Pageable.ofSize(5));

        assertEquals(2, page.getTotalElements());
        verify(currencyConversionRepository, times(1)).findByTimestampBetween(any(), any(), any());
    }

    @Test
    @DisplayName("getConversionHistory - no filter throws FilterParameterException")
    void testGetHistory_NoFilter() {
        assertThrows(
                FilterParameterException.class,
                () -> conversionService.getConversionHistory(null, null, Pageable.unpaged())
        );
    }

    @Test
    @DisplayName("bulkConvert - invalid CSV header throws InvalidCsvHeaderException")
    void testBulkConvert_InvalidHeader() {
        String csv = "source,target\nUSD,EUR";
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", csv.getBytes());

        InvalidCsvHeaderException ex = assertThrows(
                InvalidCsvHeaderException.class,
                () -> conversionService.bulkConvert(file)
        );
        assertTrue(ex.getMessage().contains("missing columns"));
    }

    @Test
    @DisplayName("bulkConvert - mixed valid and invalid rows")
    void testBulkConvert_MixedRows() throws Exception {
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.of(mockRates));
        // Mock unsupported currency
        when(currencyRepository.findByCodeAndIsActiveTrue("XXX")).thenReturn(Optional.empty());

        when(currencyConversionRepository.save(any(CurrencyConversion.class))).thenAnswer(inv -> {
            CurrencyConversion e = inv.getArgument(0);
            e.setId(UUID.randomUUID().toString());
            return e;
        });

        String csv =
                "amount,from,to\n" +
                        "100.00,USD,EUR\n" +       // Line 1: SUCCESS
                        "-5.00,USD,EUR\n" +        // Line 2: INVALID_AMOUNT
                        "50.50,USD,XXX";         // Line 3: UNSUPPORTED_CURRENCY
        MockMultipartFile file = new MockMultipartFile("file", "mix.csv", "text/csv", csv.getBytes());

        List<BulkConversionResult> results = conversionService.bulkConvert(file);

        assertEquals(3, results.size());

        // Check results by line number and error code
        assertEquals("SUCCESS", results.get(0).getCode());
        assertNotNull(results.get(0).getTransactionId());

        assertEquals("INVALID_AMOUNT", results.get(1).getCode());
        assertNull(results.get(1).getTransactionId());

        assertEquals("UNSUPPORTED_CURRENCY", results.get(2).getCode());
        assertNull(results.get(2).getTransactionId());

        verify(currencyConversionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("bulkConvert - row with rate not available")
    void testBulkConvert_RateNotAvailableForRow() throws Exception {
        Map<Object, Object> ratesWithoutTry = Map.of(
                "USD", new BigDecimal("1.2"),
                "EUR", new BigDecimal("1.0")
        );
        when(exchangeRateCacheService.getRatesMap()).thenReturn(Optional.of(ratesWithoutTry));

        when(currencyConversionRepository.save(any(CurrencyConversion.class))).thenAnswer(inv -> {
            CurrencyConversion e = inv.getArgument(0);
            e.setId(UUID.randomUUID().toString());
            return e;
        });

        String csv =
                "amount,from,to\n" +
                        "10.00,USD,EUR\n" +    // Line 1: SUCCESS
                        "20.00,USD,TRY\n";     // Line 2: RATE_NOT_AVAILABLE

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

        List<BulkConversionResult> results = conversionService.bulkConvert(file);

        assertEquals(2, results.size());

        assertEquals("SUCCESS", results.get(0).getCode());
        assertNotNull(results.get(0).getTransactionId());

        assertEquals("RATE_NOT_AVAILABLE", results.get(1).getCode());
        assertNull(results.get(1).getTransactionId());

        verify(currencyConversionRepository, times(1)).save(any());
    }
}