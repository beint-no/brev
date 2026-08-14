package no.beint.brev;

import java.time.LocalDate;
import java.util.Objects;

/** Immutable metadata identifying the exact external rules targeted by a build. */
public record PeppolRelease(
        String billingVersion,
        String validationArtifactsVersion,
        LocalDate publishedOn,
        LocalDate mandatoryFrom) {

    public static final PeppolRelease CURRENT = new PeppolRelease(
            "3.0.21",
            "1.3.16",
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 8, 17));

    public PeppolRelease {
        Values.nonBlank(billingVersion, "billing version", 32);
        Values.nonBlank(validationArtifactsVersion, "validation artefacts version", 32);
        Objects.requireNonNull(publishedOn, "publishedOn");
        Objects.requireNonNull(mandatoryFrom, "mandatoryFrom");
        if (mandatoryFrom.isBefore(publishedOn)) {
            throw new IllegalArgumentException("mandatory date must not precede publication date");
        }
    }
}
