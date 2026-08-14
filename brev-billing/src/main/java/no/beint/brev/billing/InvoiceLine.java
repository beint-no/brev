package no.beint.brev.billing;

import java.math.RoundingMode;
import java.util.Objects;

/** An invoice line whose net amount is derived deterministically from quantity and unit price. */
public record InvoiceLine(
        String id,
        String itemName,
        Quantity quantity,
        UnitPrice unitPrice,
        VatCategory vatCategory) {

    public InvoiceLine {
        BillingValues.nonBlank(id, "line ID", 64);
        BillingValues.nonBlank(itemName, "item name", 200);
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(vatCategory, "vatCategory");
    }

    public Money netAmount() {
        return new Money(
                unitPrice.currency(),
                unitPrice.amount()
                        .multiply(quantity.value())
                        .divide(unitPrice.baseQuantity(), 2, RoundingMode.HALF_UP));
    }
}
