package no.beint.brev.smp;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/** The typed result of an SMP endpoint lookup. No HTTP client is included in this release. */
public record SmpEndpoint(
        URI url,
        TransportProfile transportProfile,
        Optional<byte[]> certificate,
        Optional<String> technicalContact) {

    public SmpEndpoint {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(transportProfile, "transportProfile");
        Objects.requireNonNull(certificate, "certificate");
        certificate = certificate.map(byte[]::clone);
        Objects.requireNonNull(technicalContact, "technicalContact");
    }
}
