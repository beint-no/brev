package no.beint.brev.smp;

import no.beint.brev.DocumentTypeId;
import no.beint.brev.ParticipantId;
import no.beint.brev.ProcessId;

import java.util.List;
import java.util.Objects;

/** SMP service metadata for one participant and document type. */
public record ServiceMetadata(
        ParticipantId participant,
        DocumentTypeId documentType,
        ProcessId process,
        List<SmpEndpoint> endpoints) {

    public ServiceMetadata {
        Objects.requireNonNull(participant, "participant");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(process, "process");
        Objects.requireNonNull(endpoints, "endpoints");
        endpoints = List.copyOf(endpoints);
    }
}
