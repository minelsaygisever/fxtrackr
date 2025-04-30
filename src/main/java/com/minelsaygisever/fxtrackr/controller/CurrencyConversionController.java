package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.annotation.CurrencyConversionApi;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionRequest;
import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.service.CurrencyConversionService;
import com.minelsaygisever.fxtrackr.annotation.CurrencyCodeParam;
import com.minelsaygisever.fxtrackr.annotation.ExchangeRateApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CurrencyConversionController {
    private final CurrencyConversionService currencyConversionService;

    @GetMapping("/exchange-rate")
    @ExchangeRateApi
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam @CurrencyCodeParam String from,
            @RequestParam @CurrencyCodeParam String to
    ) {
        log.info("Received /exchange-rate request: from='{}' to='{}'", from, to);
        return ResponseEntity.ok(currencyConversionService.getExchangeRate(from, to));
    }

    @PostMapping("/convert")
    @CurrencyConversionApi
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request
    ) {
        log.info("Received /convert request: from='{}', to='{}', amount='{}'", request.getFrom(), request.getTo(), request.getAmount());
        return ResponseEntity.ok(currencyConversionService.convertAndSaveCurrency(
                request.getAmount(), request.getFrom(), request.getTo()
        ));
    }
}
