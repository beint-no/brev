package no.beint.brev.smp;

import no.beint.brev.DocumentTypeId;
import no.beint.brev.ParticipantId;
import no.beint.brev.ProcessId;
import no.beint.brev.SchemeId;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SmpTypesTest {
    @Test
    void serviceMetadataKeepsADefensiveEndpointList() {
        ServiceMetadata metadata = new ServiceMetadata(
                new ParticipantId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                new DocumentTypeId("invoice"),
                new ProcessId("billing"),
                List.of(new SmpEndpoint(
                        URI.create("https://ap.example/as4"),
                        TransportProfile.PEPPOL_AS4_V2,
                        Optional.empty(),
                        Optional.empty())));
        assertEquals(1, metadata.endpoints().size());
    }
}
