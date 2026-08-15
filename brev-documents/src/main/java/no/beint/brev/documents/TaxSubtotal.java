package no.beint.brev.documents;

import java.util.Objects;

/** A taxable amount and tax amount for one VAT category. */
public record TaxSubtotal(VatCategory category, Money taxableAmount, Money taxAmount) {
    public TaxSubtotal {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(taxableAmount, "taxableAmount");
        Objects.requireNonNull(taxAmount, "taxAmount");
        taxableAmount.requireSameCurrency(taxAmount);
    }
}
