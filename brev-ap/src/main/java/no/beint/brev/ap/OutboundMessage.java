package no.beint.brev.ap;

import no.beint.brev.CountryCode;
import no.beint.brev.DocumentTypeId;
import no.beint.brev.ParticipantId;
import no.beint.brev.ProcessId;

import java.util.Objects;

/** A payload ready for Peppol transport. The first release does not send it. */
public record OutboundMessage(
        ParticipantId sender,
        ParticipantId receiver,
        DocumentTypeId documentType,
        ProcessId process,
        CountryCode senderCountry,
        byte[] payload) {

    public OutboundMessage {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(process, "process");
        Objects.requireNonNull(senderCountry, "senderCountry");
        Objects.requireNonNull(payload, "payload");
        if (payload.length == 0) {
            throw new IllegalArgumentException("payload must not be empty");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
