package no.beint.brev.billing;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.util.Objects;

/** A non-negative document amount with at most two fractional digits. */
public record Money(CurrencyCode currency, BigDecimal amount) {
    public Money {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("money amount must not be negative in the initial positive-invoice profile");
        }
        if (fractionalDigits(amount) > 2) {
            throw new IllegalArgumentException("money amount must have at most two fractional digits");
        }
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(currency, BigDecimal.ZERO);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(currency, amount.add(other.amount));
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " and " + other.currency);
        }
    }

    public String xmlValue() {
        return amount.setScale(2).toPlainString();
    }

    private static int fractionalDigits(BigDecimal value) {
        return Math.max(0, value.stripTrailingZeros().scale());
    }
}
