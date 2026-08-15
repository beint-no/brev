package no.beint.brev.smp;

/** Peppol transport profile identifiers. The first release only names AS4 v2. */
public record TransportProfile(String value) {
    public static final TransportProfile PEPPOL_AS4_V2 =
            new TransportProfile("peppol-transport-as4-v2_0");

    public TransportProfile {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transport profile must not be blank");
        }
    }
}
