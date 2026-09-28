package com.oasis.library.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Library rules, configurable in application.properties (prefix "library").
 */
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(
        @DefaultValue("14") int loanDays,
        @DefaultValue("5") BigDecimal finePerDay,
        @DefaultValue("3") int maxActiveLoans) {
}
