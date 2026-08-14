package no.beint.brev.billing;

/** A Peppol credit-transfer instruction (payment means code 30). */
public record PaymentInstruction(String accountId, String paymentReference) {
    public PaymentInstruction {
        BillingValues.nonBlank(accountId, "payment account ID", 100);
        BillingValues.nonBlank(paymentReference, "payment reference", 100);
    }
}
