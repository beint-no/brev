package no.beint.brev.documents;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** A document amount with at most two fractional digits. Negative values are allowed for credit notes. */
public record Money(CurrencyCode currency, BigDecimal amount) {
    public Money {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
        if (fractionalDigits(amount) > 2) {
            throw new IllegalArgumentException("money amount must have at most two fractional digits");
        }
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(currency, BigDecimal.ZERO);
    }

    public static Money of(CurrencyCode currency, BigDecimal amount) {
        return new Money(currency, amount.setScale(2, RoundingMode.HALF_UP));
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
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static int fractionalDigits(BigDecimal value) {
        return Math.max(0, value.stripTrailingZeros().scale());
    }
}
