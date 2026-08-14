package no.beint.brev.billing;

import java.math.BigDecimal;
import java.util.Objects;

/** A current-profile VAT category and percentage. */
public record VatCategory(TaxCategoryCode code, BigDecimal rate) {
    public VatCategory {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(rate, "rate");
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("VAT rate must be between 0 and 100");
        }
        if (code == TaxCategoryCode.STANDARD_RATE && rate.signum() == 0) {
            throw new IllegalArgumentException("standard-rate VAT must have a positive rate");
        }
        if (code == TaxCategoryCode.ZERO_RATE && rate.signum() != 0) {
            throw new IllegalArgumentException("zero-rate VAT must have a zero rate");
        }
    }

    public String rateXmlValue() {
        return BillingValues.decimal(rate);
    }
}
