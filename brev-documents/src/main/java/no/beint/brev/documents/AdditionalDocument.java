package no.beint.brev.documents;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** An embedded supporting document. The writer Base64-encodes {@code content} incrementally. */
public record AdditionalDocument(
        String id, String mimeType, String fileName, byte[] content, Optional<String> uri) {
    public AdditionalDocument {
        BillingValues.nonBlank(id, "additional document ID", 200);
        BillingValues.nonBlank(mimeType, "mime type", 100);
        BillingValues.nonBlank(fileName, "file name", 200);
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(uri, "uri");
        uri = uri.flatMap(value -> BillingValues.optionalText(value, "document URI", 500));
        if (content.length == 0) {
            throw new IllegalArgumentException("additional document content must not be empty");
        }
        content = content.clone();
    }

    public AdditionalDocument(String id, String mimeType, String fileName, byte[] content) {
        this(id, mimeType, fileName, content, Optional.empty());
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AdditionalDocument document
                && id.equals(document.id)
                && mimeType.equals(document.mimeType)
                && fileName.equals(document.fileName)
                && Arrays.equals(content, document.content)
                && uri.equals(document.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mimeType, fileName, Arrays.hashCode(content), uri);
    }
}
