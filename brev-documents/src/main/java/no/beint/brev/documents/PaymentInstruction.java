package no.beint.brev.documents;

import java.util.Optional;

/** A Peppol credit-transfer instruction (payment means code 30). */
public record PaymentInstruction(
        String accountId,
        Optional<String> paymentReference,
        Optional<String> bic,
        Optional<String> bankName,
        Optional<String> bankCountryCode) {

    public PaymentInstruction {
        BillingValues.nonBlank(accountId, "payment account ID", 100);
        paymentReference = paymentReference.flatMap(value -> BillingValues.optionalText(value, "payment reference", 100));
        bic = bic.flatMap(value -> BillingValues.optionalText(value, "BIC", 20));
        bankName = bankName.flatMap(value -> BillingValues.optionalText(value, "bank name", 200));
        bankCountryCode = bankCountryCode.flatMap(value -> BillingValues.optionalText(value, "bank country", 2));
    }

    public PaymentInstruction(String accountId, Optional<String> paymentReference, Optional<String> bic) {
        this(accountId, paymentReference, bic, Optional.empty(), Optional.empty());
    }

    public PaymentInstruction(String accountId, String paymentReference) {
        this(accountId, Optional.of(paymentReference), Optional.empty());
    }
}
