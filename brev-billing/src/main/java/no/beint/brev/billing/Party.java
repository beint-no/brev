package no.beint.brev.billing;

import no.beint.brev.EndpointId;

import java.util.Objects;
import java.util.Optional;

/** A seller or buyer with typed endpoint identity and immutable legal details. */
public record Party(
        EndpointId endpoint,
        String legalName,
        String registrationId,
        Optional<String> vatIdentifier,
        PostalAddress address) {

    public Party {
        Objects.requireNonNull(endpoint, "endpoint");
        BillingValues.nonBlank(legalName, "legal name", 200);
        BillingValues.nonBlank(registrationId, "registration ID", 100);
        Objects.requireNonNull(vatIdentifier, "vatIdentifier");
        vatIdentifier = vatIdentifier.map(value -> BillingValues.nonBlank(value, "VAT identifier", 100));
        Objects.requireNonNull(address, "address");
    }

    public static Party withVat(
            EndpointId endpoint,
            String legalName,
            String registrationId,
            String vatIdentifier,
            PostalAddress address) {
        return new Party(endpoint, legalName, registrationId, Optional.of(vatIdentifier), address);
    }

    public static Party withoutVat(
            EndpointId endpoint,
            String legalName,
            String registrationId,
            PostalAddress address) {
        return new Party(endpoint, legalName, registrationId, Optional.empty(), address);
    }
}
