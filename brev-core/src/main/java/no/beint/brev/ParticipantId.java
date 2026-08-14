package no.beint.brev;

import java.util.Objects;

/** A Peppol participant identifier with an explicit identifier scheme. */
public record ParticipantId(SchemeId scheme, String value) {
    public ParticipantId {
        Objects.requireNonNull(scheme, "scheme");
        Values.nonBlank(value, "participant ID", 200);
    }

    public String encoded() {
        return scheme.value() + ':' + value;
    }

    @Override
    public String toString() {
        return encoded();
    }
}
