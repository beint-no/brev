package no.beint.brev.documents;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.util.Objects;

/** A non-negative price per an explicit base quantity. */
public record UnitPrice(CurrencyCode currency, BigDecimal amount, BigDecimal baseQuantity) {
    public UnitPrice {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("unit price must not be negative");
        }
        Objects.requireNonNull(baseQuantity, "base quantity");
        if (baseQuantity.signum() <= 0) {
            throw new IllegalArgumentException("base quantity must be greater than zero");
        }
    }

    public UnitPrice(CurrencyCode currency, BigDecimal amount) {
        this(currency, amount, BigDecimal.ONE);
    }

    public String amountXmlValue() {
        return BillingValues.decimal(amount);
    }

    public String baseQuantityXmlValue() {
        return BillingValues.decimal(baseQuantity);
    }
}
