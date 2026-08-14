package no.beint.brev;

import java.util.Currency;

/** An ISO 4217 currency code recognized by the running JDK. */
public record CurrencyCode(String value) {
    public CurrencyCode {
        Values.upperAsciiCode(value, "currency code", 3, 3);
        Currency.getInstance(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
