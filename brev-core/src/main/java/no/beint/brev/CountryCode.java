package no.beint.brev;

import java.util.Locale;
import java.util.Set;

/** An ISO 3166-1 alpha-2 country code recognized by the running JDK. */
public record CountryCode(String value) {
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public CountryCode {
        Values.upperAsciiCode(value, "country code", 2, 2);
        if (!ISO_COUNTRIES.contains(value)) {
            throw new IllegalArgumentException("unknown ISO 3166-1 country code: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
