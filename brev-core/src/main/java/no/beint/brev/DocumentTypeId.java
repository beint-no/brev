package no.beint.brev;

/** An exact Peppol document type identifier. */
public record DocumentTypeId(String value) {
    public DocumentTypeId {
        Values.nonBlank(value, "document type ID", 500);
    }

    @Override
    public String toString() {
        return value;
    }
}
