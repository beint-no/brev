package no.beint.brev;

/** A four-digit ISO 6523/EAS identifier scheme code. */
public record SchemeId(String value) {
    public static final SchemeId NORWEGIAN_ORGANIZATION = new SchemeId("0192");

    public SchemeId {
        Values.nonBlank(value, "scheme ID", 4);
        if (value.length() != 4 || !isAsciiDigits(value)) {
            throw new IllegalArgumentException("scheme ID must contain exactly four ASCII digits");
        }
    }

    private static boolean isAsciiDigits(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return value;
    }
}
