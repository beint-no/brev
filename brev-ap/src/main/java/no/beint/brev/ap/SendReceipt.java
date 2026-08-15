package no.beint.brev.ap;

import java.util.Objects;
import java.util.Optional;

/** Transport outcome types for a future Phase4 adapter. */
public record SendReceipt(boolean success, Optional<String> messageId, Optional<String> error) {
    public SendReceipt {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(error, "error");
    }

    public static SendReceipt ok(String messageId) {
        return new SendReceipt(true, Optional.of(messageId), Optional.empty());
    }

    public static SendReceipt failed(String error) {
        return new SendReceipt(false, Optional.empty(), Optional.of(error));
    }
}
