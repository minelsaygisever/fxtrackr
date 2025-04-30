package com.minelsaygisever.fxtrackr.repository;

import com.minelsaygisever.fxtrackr.domain.CurrencyConversion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyConversionRepository extends JpaRepository<CurrencyConversion, String> {
}
