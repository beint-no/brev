package no.beint.brev.benchmark;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;
import no.beint.brev.documents.BillingDocument;
import no.beint.brev.documents.BillingLine;
import no.beint.brev.documents.Party;
import no.beint.brev.documents.PaymentInstruction;
import no.beint.brev.documents.PostalAddress;
import no.beint.brev.documents.Quantity;
import no.beint.brev.documents.UnitPrice;
import no.beint.brev.documents.VatCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class BillingBenchmarkFixture {
    private static final CurrencyCode NOK = new CurrencyCode("NOK");

    private BillingBenchmarkFixture() {
    }

    static BillingDocument createBrevInvoice() {
        PostalAddress address = new PostalAddress("Testveien 1", "Oslo", "0150", new CountryCode("NO"));
        Party seller = Party.withVat(
                new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                "Seller AS",
                "922989451",
                "NO922989451MVA",
                address).withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION);
        Party buyer = Party.withoutVat(
                new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "987654325"),
                "Buyer AS",
                "987654325",
                address).withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION);
        return BillingDocument.invoice()
                .id("BENCH-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("benchmark")
                .seller(seller)
                .buyer(buyer)
                .payment(new PaymentInstruction("NO9386011117947", "1234567890123456789012345"))
                .line(new BillingLine(
                        "1",
                        "Consulting",
                        new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1250")),
                        VatCategory.standard(new BigDecimal("25"))))
                .build();
    }

    static peppol.bis.invoice3.domain.Invoice createDigipostInvoice() {
        peppol.bis.invoice3.domain.AccountingSupplierParty seller = new peppol.bis.invoice3.domain.AccountingSupplierParty(
                createDigipostParty("922989451", "Seller AS", true));
        peppol.bis.invoice3.domain.AccountingCustomerParty buyer = new peppol.bis.invoice3.domain.AccountingCustomerParty(
                createDigipostParty("987654325", "Buyer AS", false));
        peppol.bis.invoice3.domain.TaxScheme vatScheme = new peppol.bis.invoice3.domain.TaxScheme("VAT");
        peppol.bis.invoice3.domain.TaxCategory taxCategory = new peppol.bis.invoice3.domain.TaxCategory("S", vatScheme)
                .withPercent("25");
        peppol.bis.invoice3.domain.TaxTotal taxTotal = new peppol.bis.invoice3.domain.TaxTotal(
                new peppol.bis.invoice3.domain.TaxAmount("3125.00", "NOK"))
                .withTaxSubtotal(new peppol.bis.invoice3.domain.TaxSubtotal(
                        new peppol.bis.invoice3.domain.TaxableAmount("12500.00", "NOK"),
                        new peppol.bis.invoice3.domain.TaxAmount("3125.00", "NOK"),
                        taxCategory));
        peppol.bis.invoice3.domain.LegalMonetaryTotal totals = new peppol.bis.invoice3.domain.LegalMonetaryTotal(
                new peppol.bis.invoice3.domain.LineExtensionAmount("12500.00", "NOK"),
                new peppol.bis.invoice3.domain.TaxExclusiveAmount("12500.00", "NOK"),
                new peppol.bis.invoice3.domain.TaxInclusiveAmount("15625.00", "NOK"),
                new peppol.bis.invoice3.domain.PayableAmount("15625.00", "NOK"));
        peppol.bis.invoice3.domain.ClassifiedTaxCategory lineTax =
                new peppol.bis.invoice3.domain.ClassifiedTaxCategory("S", new peppol.bis.invoice3.domain.TaxScheme("VAT"))
                        .withPercent("25");
        peppol.bis.invoice3.domain.InvoiceLine line = new peppol.bis.invoice3.domain.InvoiceLine(
                "1",
                new peppol.bis.invoice3.domain.InvoicedQuantity("10", "HUR"),
                new peppol.bis.invoice3.domain.LineExtensionAmount("12500.00", "NOK"),
                new peppol.bis.invoice3.domain.Item("Consulting", lineTax),
                new peppol.bis.invoice3.domain.Price(new peppol.bis.invoice3.domain.PriceAmount("1250", "NOK"))
                        .withBaseQuantity(new peppol.bis.invoice3.domain.BaseQuantity("1").withUnitCode("HUR")));
        peppol.bis.invoice3.domain.PaymentMeans payment = new peppol.bis.invoice3.domain.PaymentMeans(
                new peppol.bis.invoice3.domain.PaymentMeansCode("30").withName("Credit transfer"))
                .withPaymentID("1234567890123456789012345")
                .withPayeeFinancialAccount(new peppol.bis.invoice3.domain.PayeeFinancialAccount("NO9386011117947"));
        return new peppol.bis.invoice3.domain.Invoice(
                "BENCH-1", "2026-08-17", "NOK", seller, buyer, taxTotal, totals, List.of(line))
                .withDueDate("2026-09-01")
                .withBuyerReference("benchmark")
                .withPaymentMeans(payment);
    }

    private static peppol.bis.invoice3.domain.Party createDigipostParty(
            String organizationNumber, String name, boolean vatRegistered) {
        peppol.bis.invoice3.domain.PostalAddress address = new peppol.bis.invoice3.domain.PostalAddress(
                new peppol.bis.invoice3.domain.Country("NO"))
                .withStreetName("Testveien 1")
                .withCityName("Oslo")
                .withPostalZone("0150");
        peppol.bis.invoice3.domain.Party party = new peppol.bis.invoice3.domain.Party(
                new peppol.bis.invoice3.domain.EndpointID(organizationNumber).withSchemeID("0192"),
                address,
                new peppol.bis.invoice3.domain.PartyLegalEntity(name)
                        .withCompanyID(new peppol.bis.invoice3.domain.CompanyID(organizationNumber).withSchemeID("0192")))
                .withPartyName(new peppol.bis.invoice3.domain.PartyName(name));
        if (vatRegistered) {
            party.withPartyTaxScheme(new peppol.bis.invoice3.domain.PartyTaxScheme(
                    "NO" + organizationNumber + "MVA", new peppol.bis.invoice3.domain.TaxScheme("VAT")));
        }
        return party;
    }
}
