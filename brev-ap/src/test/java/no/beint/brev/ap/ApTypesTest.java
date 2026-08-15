package no.beint.brev.ap;

import no.beint.brev.CountryCode;
import no.beint.brev.DocumentTypeId;
import no.beint.brev.ParticipantId;
import no.beint.brev.ProcessId;
import no.beint.brev.SchemeId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ApTypesTest {
    @Test
    void outboundMessageRejectsEmptyPayloadByConstruction() {
        OutboundMessage message = new OutboundMessage(
                new ParticipantId(SchemeId.NORWEGIAN_ORGANIZATION, "1"),
                new ParticipantId(SchemeId.NORWEGIAN_ORGANIZATION, "2"),
                new DocumentTypeId("invoice"),
                new ProcessId("billing"),
                new CountryCode("NO"),
                new byte[] {1});
        assertTrue(SendReceipt.ok("mid-1").success());
        assertTrue(message.payload().length == 1);
    }
}
