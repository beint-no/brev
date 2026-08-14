package no.beint.brev;

import java.util.Objects;

/** An electronic address used in a Peppol business document. */
public record EndpointId(SchemeId scheme, String value) {
    public EndpointId {
        Objects.requireNonNull(scheme, "scheme");
        Values.nonBlank(value, "endpoint ID", 200);
    }

    @Override
    public String toString() {
        return scheme.value() + ':' + value;
    }
}
