package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import com.minelsaygisever.fxtrackr.dto.BulkConversionResult;
import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.exception.*;
import com.minelsaygisever.fxtrackr.mapper.ConversionMapper;
import com.minelsaygisever.fxtrackr.repository.CurrencyConversionRepository;
import com.minelsaygisever.fxtrackr.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final FixerRestClient fixerRestClient;
    private final CurrencyConversionRepository currencyConversionRepository;
    @Autowired
    private ValidationUtil validationUtil;
    @Autowired
    private ConversionMapper conversionMapper;

    public ExchangeRateResponse getExchangeRate(String from, String to) {
        String fromNorm = validationUtil.validateAndNormalizeCurrencyCode(from);
        String toNorm = validationUtil.validateAndNormalizeCurrencyCode(to);

        BigDecimal rate = fixerRestClient.getRate(fromNorm, toNorm);
        return ExchangeRateResponse.builder()
                .exchangeRate(rate)
                .build();
    }

    @Transactional
    public CurrencyConversionResponse convertAndSaveCurrency(BigDecimal amount, String from, String to) {
        BigDecimal amountNorm = validationUtil.validateAndNormalizeAmount(amount);
        String fromNorm = validationUtil.validateAndNormalizeCurrencyCode(from);
        String toNorm = validationUtil.validateAndNormalizeCurrencyCode(to);

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
                                Collections.singletonList(conversionMapper.toHistoryResponse(entity)),
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
                    .map(conversionMapper::toHistoryResponse);
        }

        throw new FilterParameterException("Either transactionId or date must be provided");
    }

    @Transactional
    public List<BulkConversionResult> bulkConvert(MultipartFile file) {
        List<BulkConversionResult> results = new ArrayList<>();
        try (
                Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser csvParser = CSVFormat.DEFAULT
                        .withHeader("amount","from","to")
                        .withFirstRecordAsHeader()
                        .parse(reader)
        ) {
            validationUtil.validateCsvHeaders(csvParser);

            int line = 1;
            for (CSVRecord record : csvParser) {
                BulkConversionResult.BulkConversionResultBuilder bulkConversionResultBuilder =
                        BulkConversionResult.builder().line(line);
                try {
                    BigDecimal amount = new BigDecimal(record.get("amount").trim());
                    String from = record.get("from").trim();
                    String to   = record.get("to").trim();

                    // Reuse existing convertAndSaveCurrency()
                    CurrencyConversionResponse currencyConversionResponse =
                            convertAndSaveCurrency(amount, from, to);

                    bulkConversionResultBuilder.transactionId(currencyConversionResponse.getTransactionId())
                            .convertedAmount(currencyConversionResponse.getConvertedAmount())
                            .code("SUCCESS")
                            .message("OK");

                } catch (CurrencyNotFoundException e) {
                    bulkConversionResultBuilder.code("INVALID_CURRENCY").message(e.getMessage());
                } catch (InvalidAmountException e) {
                    bulkConversionResultBuilder.code("INVALID_AMOUNT").message(e.getMessage());
                } catch (ExternalApiException e) {
                    bulkConversionResultBuilder.code("EXTERNAL_API_ERROR").message(e.getMessage());
                } catch (Exception e) {
                    bulkConversionResultBuilder.code("PROCESSING_ERROR").message(e.getMessage());
                }
                results.add(bulkConversionResultBuilder.build());
                line++;
            }
            return results;
        } catch (InvalidCsvHeaderException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BulkProcessingException("Failed to process bulk CSV", e);
        }
    }
}
