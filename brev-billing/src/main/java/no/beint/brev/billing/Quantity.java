package no.beint.brev.billing;

import no.beint.brev.UnitCode;

import java.math.BigDecimal;
import java.util.Objects;

/** A strictly positive quantity for the initial positive-invoice profile. */
public record Quantity(BigDecimal value, UnitCode unit) {
    public Quantity {
        BillingValues.positive(value, "quantity");
        Objects.requireNonNull(unit, "unit");
    }

    public String xmlValue() {
        return BillingValues.decimal(value);
    }
}
