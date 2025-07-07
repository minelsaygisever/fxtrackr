package com.minelsaygisever.fxtrackr.domain;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents a currency supported by the system.
 * This entity is mapped to the 'currencies' table in the database.
 */
@Entity
@Table(name = "CURRENCY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

    /**
     * The unique ISO 4217 code for the currency (e.g., "USD", "EUR").
     * This serves as the primary key.
     */
    @Id
    @Column(name = "CODE", length = 3, nullable = false, unique = true)
    private String code;

    /**
     * The full name of the currency (e.g., "United States Dollar").
     */
    @Column(name = "NAME", nullable = false)
    private String name;

    /**
     * A flag to indicate if the currency is actively supported for conversions.
     * This allows for dynamically enabling/disabling currencies.
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive;
}
