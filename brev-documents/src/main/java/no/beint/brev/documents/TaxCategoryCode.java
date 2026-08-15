package no.beint.brev.documents;

import java.util.Locale;
import java.util.Optional;

/** VAT category codes used by the current Peppol Billing profile. */
public enum TaxCategoryCode {
    STANDARD_RATE("S", true, false),
    ZERO_RATE("Z", true, false),
    EXEMPT("E", true, true),
    EXPORT("G", true, true),
    REVERSE_CHARGE("AE", true, true),
    OUTSIDE_SCOPE("O", false, true);

    private final String xmlValue;
    private final boolean rateRequired;
    private final boolean exemptionRequired;

    TaxCategoryCode(String xmlValue, boolean rateRequired, boolean exemptionRequired) {
        this.xmlValue = xmlValue;
        this.rateRequired = rateRequired;
        this.exemptionRequired = exemptionRequired;
    }

    public String xmlValue() {
        return xmlValue;
    }

    public boolean rateRequired() {
        return rateRequired;
    }

    public boolean exemptionRequired() {
        return exemptionRequired;
    }

    public static Optional<TaxCategoryCode> fromXmlValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        for (TaxCategoryCode code : values()) {
            if (code.xmlValue.equals(normalized)) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }
}
