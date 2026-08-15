package no.beint.brev.documents;

import java.util.Objects;
import java.util.Optional;

/** A document-level allowance or charge preserved from inbound UBL. */
public record DocumentAllowanceCharge(
        boolean charge,
        Optional<String> reason,
        Money amount,
        Optional<VatCategory> vat) {

    public DocumentAllowanceCharge {
        Objects.requireNonNull(reason, "reason");
        reason = reason.flatMap(value -> BillingValues.optionalText(value, "allowance reason", 200));
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(vat, "vat");
    }
}
