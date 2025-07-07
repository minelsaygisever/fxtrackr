package com.minelsaygisever.fxtrackr.repository;

import com.minelsaygisever.fxtrackr.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    Optional<Currency> findByCodeAndIsActiveTrue(String code);

}
