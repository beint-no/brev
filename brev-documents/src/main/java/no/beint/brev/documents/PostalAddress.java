package no.beint.brev.documents;

import no.beint.brev.CountryCode;

import java.util.Objects;
import java.util.Optional;

/** A postal address. Only the country is mandatory; empty optional parts are omitted from XML. */
public record PostalAddress(
        Optional<String> streetName,
        Optional<String> additionalStreetName,
        Optional<String> cityName,
        Optional<String> postalZone,
        CountryCode country) {

    public PostalAddress {
        Objects.requireNonNull(streetName, "streetName");
        Objects.requireNonNull(additionalStreetName, "additionalStreetName");
        Objects.requireNonNull(cityName, "cityName");
        Objects.requireNonNull(postalZone, "postalZone");
        Objects.requireNonNull(country, "country");
        streetName = streetName.flatMap(value -> BillingValues.optionalText(value, "street name", 200));
        additionalStreetName =
                additionalStreetName.flatMap(value -> BillingValues.optionalText(value, "additional street name", 200));
        cityName = cityName.flatMap(value -> BillingValues.optionalText(value, "city name", 100));
        postalZone = postalZone.flatMap(value -> BillingValues.optionalText(value, "postal zone", 32));
    }

    public PostalAddress(String streetName, String cityName, String postalZone, CountryCode country) {
        this(Optional.of(streetName), Optional.empty(), Optional.of(cityName), Optional.of(postalZone), country);
    }

    public PostalAddress(
            String streetName,
            String additionalStreetName,
            String cityName,
            String postalZone,
            CountryCode country) {
        this(
                Optional.ofNullable(streetName),
                Optional.ofNullable(additionalStreetName),
                Optional.ofNullable(cityName),
                Optional.ofNullable(postalZone),
                country);
    }

    public static PostalAddress ofCountry(CountryCode country) {
        return new PostalAddress(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), country);
    }
}
