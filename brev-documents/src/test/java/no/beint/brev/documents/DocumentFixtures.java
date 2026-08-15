package no.beint.brev.documents;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

final class DocumentFixtures {
    static final CurrencyCode NOK = new CurrencyCode("NOK");

    private DocumentFixtures() {
    }

    static Party seller() {
        return Party.withVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "913341464"),
                        "Brev & Sønner AS",
                        "913341464",
                        "NO913341464MVA",
                        new PostalAddress("Dokumentveien 1", "Oslo", "0150", new CountryCode("NO")))
                .withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION)
                .withForetaksregisteret();
    }

    static Party buyer() {
        return Party.withoutVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "987654321"),
                        "Kjøper AS",
                        "987654321",
                        new PostalAddress(
                                java.util.Optional.of("Testgata 2"),
                                java.util.Optional.of("C/O Post"),
                                java.util.Optional.of("Bergen"),
                                java.util.Optional.of("5003"),
                                new CountryCode("NO")))
                .withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION);
    }

    static BillingDocument invoice() {
        return BillingDocument.invoice()
                .id("INV-2026-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("buyer@example.no")
                .orderReference(OrderReference.salesOrder("2026-100"))
                .seller(seller())
                .buyer(buyer())
                .payment(new PaymentInstruction("NO9386011117947", "1234567890123456789012345"))
                .line(new BillingLine(
                        "1",
                        "Rådgivning <senior>",
                        new Quantity(new BigDecimal("2.5"), new UnitCode("HUR")),
                        new UnitPrice(NOK, new BigDecimal("1000")),
                        VatCategory.standard(new BigDecimal("25"))))
                .line(new BillingLine(
                        "2",
                        "Rapport 📄",
                        new Quantity(BigDecimal.ONE, UnitCode.EACH),
                        new UnitPrice(NOK, new BigDecimal("400.50")),
                        VatCategory.zero()))
                .build();
    }

    static BillingDocument creditNote() {
        return BillingDocument.creditNote()
                .id("CN-2026-1")
                .issueDate(LocalDate.of(2026, 8, 18))
                .currency(NOK)
                .orderReference(OrderReference.salesOrder("2026-100"))
                .referencedInvoiceId("INV-2026-1")
                .seller(seller())
                .buyer(buyer())
                .payment(new PaymentInstruction("NO9386011117947", java.util.Optional.of("CN-2026-1"), java.util.Optional.of("DNBANOKK")))
                .line(new BillingLine(
                        "1",
                        "Kreditert rådgivning",
                        new Quantity(new BigDecimal("2.5"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1000")),
                        VatCategory.standard(new BigDecimal("25"))))
                .build();
    }

    static BillingDocument norwegianInvoiceWithAttachment() {
        return BillingDocument.invoice()
                .id("INV-NO-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 8, 31))
                .currency(NOK)
                .orderReference(OrderReference.salesOrder("SO-1"))
                .seller(seller())
                .buyer(buyer())
                .payment(new PaymentInstruction(
                        "NO9386011117947",
                        java.util.Optional.of("INV-NO-1"),
                        java.util.Optional.of("DNBANOKK")))
                .additionalDocument(new AdditionalDocument(
                        "invoice-pdf.pdf",
                        "application/pdf",
                        "Invoice-INV-NO-1.pdf",
                        "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII)))
                .line(new BillingLine(
                        "1",
                        "Konsulenttime",
                        new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1250")),
                        VatCategory.standard(new BigDecimal("25")),
                        Money.of(NOK, new BigDecimal("12500.00")),
                        java.util.Optional.of(Money.of(NOK, new BigDecimal("50.00")))))
                .build();
    }

    static BillingDocument mixedVatInvoice() {
        return BillingDocument.invoice()
                .id("INV-VAT-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("vat-routing")
                .seller(seller())
                .buyer(buyer())
                .line(new BillingLine(
                        "1",
                        "Standard",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("100")),
                        VatCategory.standard(new BigDecimal("25"))))
                .line(new BillingLine(
                        "2",
                        "Exempt",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("40")),
                        VatCategory.exempt("Exempt from VAT")))
                .line(new BillingLine(
                        "3",
                        "Export",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("30")),
                        VatCategory.export("Export outside the EU")))
                .line(new BillingLine(
                        "4",
                        "Reverse",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("20")),
                        VatCategory.reverseCharge("Reverse charge")))
                .line(new BillingLine(
                        "5",
                        "Outside",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("10")),
                        VatCategory.outsideScope("Not subject to VAT")))
                .build();
    }
}
