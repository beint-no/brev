package no.beint.brev.documents;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/** Facade for the current Peppol Billing document module. */
public final class Documents {
    private Documents() {
    }

    public static void write(BillingDocument document, OutputStream destination) throws IOException {
        BillingWriter.write(document, destination);
    }

    public static byte[] toByteArray(BillingDocument document) {
        return BillingWriter.toByteArray(document);
    }

    public static BillingDocument read(byte[] xml) {
        return BillingReader.read(xml);
    }

    public static Optional<BillingDocument> tryRead(byte[] xml) {
        return BillingReader.tryRead(xml);
    }
}
