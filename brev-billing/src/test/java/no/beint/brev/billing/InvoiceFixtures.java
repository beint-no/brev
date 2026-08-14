package no.beint.brev.billing;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class InvoiceFixtures {
    static final CurrencyCode NOK = new CurrencyCode("NOK");

    private InvoiceFixtures() {
    }

    static Invoice invoice() {
        Party seller = Party.withVat(
                new EndpointId(new SchemeId("0192"), "913341464"),
                "Brev & Sønner AS",
                "913341464",
                "NO913341464MVA",
                new PostalAddress("Dokumentveien 1", "Oslo", "0150", new CountryCode("NO")));
        Party buyer = Party.withoutVat(
                new EndpointId(new SchemeId("0192"), "987654321"),
                "Kjøper AS",
                "987654321",
                new PostalAddress("Testgata 2", "Bergen", "5003", new CountryCode("NO")));

        return new Invoice(
                "INV-2026-1",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 9, 1),
                NOK,
                "buyer@example.no",
                seller,
                buyer,
                new PaymentInstruction("NO9386011117947", "1234567890123456789012345"),
                List.of(
                        new InvoiceLine(
                                "1",
                                "Rådgivning <senior>",
                                new Quantity(new BigDecimal("2.5"), new UnitCode("HUR")),
                                new UnitPrice(NOK, new BigDecimal("1000")),
                                new VatCategory(TaxCategoryCode.STANDARD_RATE, new BigDecimal("25"))),
                        new InvoiceLine(
                                "2",
                                "Rapport 📄",
                                new Quantity(BigDecimal.ONE, UnitCode.EACH),
                                new UnitPrice(NOK, new BigDecimal("400.50")),
                                new VatCategory(TaxCategoryCode.ZERO_RATE, BigDecimal.ZERO))));
    }
}
