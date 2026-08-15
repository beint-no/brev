package no.beint.brev.documents;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

final class BillingValues {
    private BillingValues() {
    }

    static String nonBlank(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (!value.equals(value.strip())) {
            throw new IllegalArgumentException(name + " must not have surrounding whitespace");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(name + " exceeds " + maximumLength + " characters");
        }
        return value;
    }

    static Optional<String> optionalText(String value, String name, int maximumLength) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(nonBlank(value, name, maximumLength));
    }

    static String decimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
}
