package no.beint.brev.billing;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.util.Objects;

/** A price per an explicit base quantity. Price precision is not limited to two decimals. */
public record UnitPrice(CurrencyCode currency, BigDecimal amount, BigDecimal baseQuantity) {
    public UnitPrice {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("unit price must not be negative");
        }
        BillingValues.positive(baseQuantity, "base quantity");
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
