package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.annotation.*;
import com.minelsaygisever.fxtrackr.dto.*;
import com.minelsaygisever.fxtrackr.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;


@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CurrencyConversionController {
    private final CurrencyConversionService currencyConversionService;

    @ExchangeRateApi
    @GetMapping("/exchange-rate")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam @CurrencyCodeParam String from,
            @RequestParam @CurrencyCodeParam String to
    ) {
        log.info("Received /exchange-rate request: from='{}' to='{}'", from, to);
        return ResponseEntity.ok(currencyConversionService.getExchangeRate(from, to));
    }

    @CurrencyConversionApi
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request
    ) {
        log.info("Received /convert request: from='{}', to='{}', amount='{}'", request.getFrom(), request.getTo(), request.getAmount());
        return ResponseEntity.ok(currencyConversionService.convertAndSaveCurrency(
                request.getAmount(), request.getFrom(), request.getTo()
        ));
    }

    @SearchHistoryApi
    @PostMapping("/conversions/search")
    public ResponseEntity<Page<ConversionHistoryResponse>> searchHistory(
            @Valid @RequestBody ConversionHistoryRequest request,
            Pageable pageable
    ) {
        log.info("Received /conversions/search: txId={}, date={}, page={}",
                request.getTransactionId(), request.getDate(), pageable);
        Page<ConversionHistoryResponse> page = currencyConversionService.getConversionHistory(
                request.getTransactionId(),
                request.getDate(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @BulkConvertApi
    @PostMapping(
            value    = "/convert/bulk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<BulkConversionResult>> bulkConvert(
            @RequestPart("file") MultipartFile file
    ) {
        log.info("Received bulk CSV conversion: {}", file.getOriginalFilename());
        List<BulkConversionResult> results = currencyConversionService.bulkConvert(file);
        return ResponseEntity.ok(results);
    }
}
