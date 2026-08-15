package no.beint.brev.documents;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** A current-profile VAT category with the fields that category requires. */
public record VatCategory(TaxCategoryCode code, Optional<BigDecimal> rate, Optional<String> exemptionReason) {
    public VatCategory {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(exemptionReason, "exemptionReason");
        rate = rate.map(value -> {
            if (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("VAT rate must be between 0 and 100");
            }
            return value;
        });
        exemptionReason = exemptionReason.map(value -> BillingValues.nonBlank(value, "exemption reason", 200));
        if (code.rateRequired() && rate.isEmpty()) {
            throw new IllegalArgumentException(code + " requires a VAT rate");
        }
        if (!code.rateRequired() && rate.isPresent()) {
            throw new IllegalArgumentException(code + " must not include a VAT rate");
        }
        if (code == TaxCategoryCode.STANDARD_RATE && rate.orElseThrow().signum() == 0) {
            throw new IllegalArgumentException("standard-rate VAT must have a positive rate");
        }
        if (code == TaxCategoryCode.ZERO_RATE && rate.orElseThrow().signum() != 0) {
            throw new IllegalArgumentException("zero-rate VAT must have a zero rate");
        }
        if ((code == TaxCategoryCode.EXEMPT
                || code == TaxCategoryCode.EXPORT
                || code == TaxCategoryCode.REVERSE_CHARGE)
                && rate.orElseThrow().signum() != 0) {
            throw new IllegalArgumentException(code + " must have a zero rate");
        }
        if (code.exemptionRequired() && exemptionReason.isEmpty()) {
            throw new IllegalArgumentException(code + " requires an exemption reason");
        }
        if (!code.exemptionRequired() && exemptionReason.isPresent()) {
            throw new IllegalArgumentException(code + " must not include an exemption reason");
        }
    }

    public VatCategory(TaxCategoryCode code, BigDecimal rate) {
        this(code, Optional.of(rate), Optional.empty());
    }

    public static VatCategory standard(BigDecimal rate) {
        return new VatCategory(TaxCategoryCode.STANDARD_RATE, Optional.of(rate), Optional.empty());
    }

    public static VatCategory zero() {
        return new VatCategory(TaxCategoryCode.ZERO_RATE, Optional.of(BigDecimal.ZERO), Optional.empty());
    }

    public static VatCategory exempt(String reason) {
        return new VatCategory(TaxCategoryCode.EXEMPT, Optional.of(BigDecimal.ZERO), Optional.of(reason));
    }

    public static VatCategory export(String reason) {
        return new VatCategory(TaxCategoryCode.EXPORT, Optional.of(BigDecimal.ZERO), Optional.of(reason));
    }

    public static VatCategory reverseCharge(String reason) {
        return new VatCategory(TaxCategoryCode.REVERSE_CHARGE, Optional.of(BigDecimal.ZERO), Optional.of(reason));
    }

    public static VatCategory outsideScope(String reason) {
        return new VatCategory(TaxCategoryCode.OUTSIDE_SCOPE, Optional.empty(), Optional.of(reason));
    }

    public String rateXmlValue() {
        return BillingValues.decimal(rate.orElseThrow(() -> new IllegalStateException("VAT category has no rate")));
    }
}
