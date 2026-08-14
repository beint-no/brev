package no.beint.brev.billing;

import no.beint.brev.CountryCode;

import java.util.Objects;

/** The required address fields in Brev's initial Billing profile. */
public record PostalAddress(String streetName, String cityName, String postalZone, CountryCode country) {
    public PostalAddress {
        BillingValues.nonBlank(streetName, "street name", 200);
        BillingValues.nonBlank(cityName, "city name", 100);
        BillingValues.nonBlank(postalZone, "postal zone", 32);
        Objects.requireNonNull(country, "country");
    }
}
