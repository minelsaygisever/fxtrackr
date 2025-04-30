package com.minelsaygisever.fxtrackr.domain;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "CURRENCY_CONVERSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyConversion {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private String id;

    @Column(name = "SOURCE_CURRENCY", nullable = false)
    private String sourceCurrency;

    @Column(name = "TARGET_CURRENCY", nullable = false)
    private String targetCurrency;

    @Column(name = "SOURCE_AMOUNT", precision = 19, scale = 6, nullable = false)
    private BigDecimal sourceAmount;

    @Column(name = "CONVERTED_AMOUNT", precision=19, scale=6, nullable = false)
    private BigDecimal convertedAmount;

    @Column(name = "EXCHANGE_RATE", precision=19, scale=6, nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "TIMESTAMP", nullable = false)
    private Instant timestamp;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
}
