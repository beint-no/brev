package no.beint.brev;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ValueTypesTest {
    @Test
    void participantAndEndpointRemainDistinctTypedValues() {
        SchemeId scheme = new SchemeId("0192");

        assertEquals("0192:123456789", new ParticipantId(scheme, "123456789").encoded());
        assertEquals("0192:123456789", new EndpointId(scheme, "123456789").toString());
    }

    @Test
    void rejectsMalformedCodesAtTheBoundary() {
        assertThrows(IllegalArgumentException.class, () -> new SchemeId("NO:ORG"));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyCode("nok"));
        assertThrows(IllegalArgumentException.class, () -> new CountryCode("XX"));
        assertThrows(IllegalArgumentException.class, () -> new UnitCode("c62"));
    }

    @Test
    void identifiesTheOnlyBundledRelease() {
        assertEquals("3.0.21", PeppolRelease.CURRENT.billingVersion());
        assertEquals("1.3.16", PeppolRelease.CURRENT.validationArtifactsVersion());
        assertEquals(LocalDate.of(2026, 8, 17), PeppolRelease.CURRENT.mandatoryFrom());
    }
}
