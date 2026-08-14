package no.beint.brev.billing;

/** Tax categories supported by Brev's first positive-invoice vertical slice. */
public enum TaxCategoryCode {
    STANDARD_RATE("S"),
    ZERO_RATE("Z");

    private final String xmlValue;

    TaxCategoryCode(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String xmlValue() {
        return xmlValue;
    }
}
