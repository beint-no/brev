package no.beint.brev.billing;

import java.util.Objects;

/** A computed taxable amount and tax amount for one VAT category and rate. */
public record TaxSubtotal(VatCategory category, Money taxableAmount, Money taxAmount) {
    public TaxSubtotal {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(taxableAmount, "taxableAmount");
        Objects.requireNonNull(taxAmount, "taxAmount");
        taxableAmount.requireSameCurrency(taxAmount);
    }
}
