package no.beint.brev.documents;

import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;

import java.util.Objects;
import java.util.Optional;

/** A seller or buyer. Endpoints are required when constructing a document for writing. */
public record Party(
        Optional<EndpointId> endpoint,
        String legalName,
        Optional<String> registrationId,
        Optional<SchemeId> registrationScheme,
        Optional<String> vatIdentifier,
        boolean registeredInForetaksregisteret,
        PostalAddress address) {

    public Party {
        Objects.requireNonNull(endpoint, "endpoint");
        BillingValues.nonBlank(legalName, "legal name", 200);
        Objects.requireNonNull(registrationId, "registrationId");
        Objects.requireNonNull(registrationScheme, "registrationScheme");
        Objects.requireNonNull(vatIdentifier, "vatIdentifier");
        registrationId = registrationId.flatMap(value -> BillingValues.optionalText(value, "registration ID", 100));
        vatIdentifier = vatIdentifier.flatMap(value -> BillingValues.optionalText(value, "VAT identifier", 100));
        Objects.requireNonNull(address, "address");
        if (registrationScheme.isPresent() && registrationId.isEmpty()) {
            throw new IllegalArgumentException("registration scheme requires a registration ID");
        }
    }

    public static Party withVat(
            EndpointId endpoint,
            String legalName,
            String registrationId,
            String vatIdentifier,
            PostalAddress address) {
        return new Party(
                Optional.of(endpoint),
                legalName,
                Optional.of(registrationId),
                Optional.empty(),
                Optional.of(vatIdentifier),
                false,
                address);
    }

    public static Party withoutVat(
            EndpointId endpoint,
            String legalName,
            String registrationId,
            PostalAddress address) {
        return new Party(
                Optional.of(endpoint),
                legalName,
                Optional.of(registrationId),
                Optional.empty(),
                Optional.empty(),
                false,
                address);
    }

    public Party withRegistrationScheme(SchemeId scheme) {
        return new Party(
                endpoint,
                legalName,
                registrationId,
                Optional.of(scheme),
                vatIdentifier,
                registeredInForetaksregisteret,
                address);
    }

    public Party withForetaksregisteret() {
        return new Party(
                endpoint,
                legalName,
                registrationId,
                registrationScheme,
                vatIdentifier,
                true,
                address);
    }
}
