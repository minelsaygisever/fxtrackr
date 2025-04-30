package com.minelsaygisever.fxtrackr.repository;

import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface CurrencyConversionRepository extends JpaRepository<CurrencyConversion, String> {
    Page<CurrencyConversion> findByTimestampBetween(Instant start, Instant end, Pageable pageable);
}
