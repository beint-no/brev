package no.beint.brev.documents;

import no.beint.brev.UnitCode;

import java.math.BigDecimal;
import java.util.Objects;

/** A quantity that may be negative on credit notes. */
public record Quantity(BigDecimal value, UnitCode unit) {
    public Quantity {
        Objects.requireNonNull(value, "quantity");
        Objects.requireNonNull(unit, "unit");
    }

    public String xmlValue() {
        return BillingValues.decimal(value);
    }
}
