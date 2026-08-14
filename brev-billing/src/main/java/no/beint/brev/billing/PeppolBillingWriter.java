package no.beint.brev.billing;

import no.beint.brev.billing.internal.Utf8XmlOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Writes the supported Billing model directly as compact UTF-8 UBL 2.1 XML. */
public final class PeppolBillingWriter {
    public static final String CUSTOMIZATION_ID =
            "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
    public static final String PROFILE_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";

    private PeppolBillingWriter() {
    }

    public static void write(Invoice invoice, OutputStream destination) throws IOException {
        Objects.requireNonNull(invoice, "invoice");
        Utf8XmlOutput xml = new Utf8XmlOutput(destination);
        xml.declaration();
        xml.raw("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"");
        xml.raw(" xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"");
        xml.raw(" xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">");

        xml.element("cbc:CustomizationID", CUSTOMIZATION_ID);
        xml.element("cbc:ProfileID", PROFILE_ID);
        xml.element("cbc:ID", invoice.id());
        xml.element("cbc:IssueDate", invoice.issueDate().toString());
        xml.element("cbc:DueDate", invoice.dueDate().toString());
        xml.element("cbc:InvoiceTypeCode", "380");
        xml.element("cbc:DocumentCurrencyCode", invoice.currency().value());
        xml.element("cbc:BuyerReference", invoice.buyerReference());

        writeParty(xml, "cac:AccountingSupplierParty", invoice.seller());
        writeParty(xml, "cac:AccountingCustomerParty", invoice.buyer());
        writePayment(xml, invoice.payment());
        writeTaxes(xml, invoice);
        writeTotals(xml, invoice);
        for (InvoiceLine line : invoice.lines()) {
            writeLine(xml, line);
        }

        xml.end("Invoice");
        xml.finish();
    }

    public static byte[] toByteArray(Invoice invoice) {
        ByteArrayOutputStream destination = new ByteArrayOutputStream(4096);
        try {
            write(invoice, destination);
        } catch (IOException exception) {
            throw new UncheckedIOException("unexpected in-memory write failure", exception);
        }
        return destination.toByteArray();
    }

    private static void writeParty(Utf8XmlOutput xml, String wrapper, Party party) throws IOException {
        xml.start(wrapper);
        xml.start("cac:Party");
        xml.element("cbc:EndpointID", "schemeID", party.endpoint().scheme().value(), party.endpoint().value());
        xml.start("cac:PartyName");
        xml.element("cbc:Name", party.legalName());
        xml.end("cac:PartyName");
        writeAddress(xml, party.address());
        if (party.vatIdentifier().isPresent()) {
            xml.start("cac:PartyTaxScheme");
            xml.element("cbc:CompanyID", party.vatIdentifier().orElseThrow());
            xml.start("cac:TaxScheme");
            xml.element("cbc:ID", "VAT");
            xml.end("cac:TaxScheme");
            xml.end("cac:PartyTaxScheme");
        }
        xml.start("cac:PartyLegalEntity");
        xml.element("cbc:RegistrationName", party.legalName());
        xml.element("cbc:CompanyID", party.registrationId());
        xml.end("cac:PartyLegalEntity");
        xml.end("cac:Party");
        xml.end(wrapper);
    }

    private static void writeAddress(Utf8XmlOutput xml, PostalAddress address) throws IOException {
        xml.start("cac:PostalAddress");
        xml.element("cbc:StreetName", address.streetName());
        xml.element("cbc:CityName", address.cityName());
        xml.element("cbc:PostalZone", address.postalZone());
        xml.start("cac:Country");
        xml.element("cbc:IdentificationCode", address.country().value());
        xml.end("cac:Country");
        xml.end("cac:PostalAddress");
    }

    private static void writePayment(Utf8XmlOutput xml, PaymentInstruction payment) throws IOException {
        xml.start("cac:PaymentMeans");
        xml.element("cbc:PaymentMeansCode", "name", "Credit transfer", "30");
        xml.element("cbc:PaymentID", payment.paymentReference());
        xml.start("cac:PayeeFinancialAccount");
        xml.element("cbc:ID", payment.accountId());
        xml.end("cac:PayeeFinancialAccount");
        xml.end("cac:PaymentMeans");
    }

    private static void writeTaxes(Utf8XmlOutput xml, Invoice invoice) throws IOException {
        xml.start("cac:TaxTotal");
        amount(xml, "cbc:TaxAmount", invoice.taxTotal());
        for (TaxSubtotal subtotal : invoice.taxSubtotals()) {
            xml.start("cac:TaxSubtotal");
            amount(xml, "cbc:TaxableAmount", subtotal.taxableAmount());
            amount(xml, "cbc:TaxAmount", subtotal.taxAmount());
            xml.start("cac:TaxCategory");
            xml.element("cbc:ID", subtotal.category().code().xmlValue());
            xml.element("cbc:Percent", subtotal.category().rateXmlValue());
            taxScheme(xml);
            xml.end("cac:TaxCategory");
            xml.end("cac:TaxSubtotal");
        }
        xml.end("cac:TaxTotal");
    }

    private static void writeTotals(Utf8XmlOutput xml, Invoice invoice) throws IOException {
        xml.start("cac:LegalMonetaryTotal");
        amount(xml, "cbc:LineExtensionAmount", invoice.lineExtensionTotal());
        amount(xml, "cbc:TaxExclusiveAmount", invoice.lineExtensionTotal());
        amount(xml, "cbc:TaxInclusiveAmount", invoice.payableAmount());
        amount(xml, "cbc:PayableAmount", invoice.payableAmount());
        xml.end("cac:LegalMonetaryTotal");
    }

    private static void writeLine(Utf8XmlOutput xml, InvoiceLine line) throws IOException {
        xml.start("cac:InvoiceLine");
        xml.element("cbc:ID", line.id());
        xml.element(
                "cbc:InvoicedQuantity",
                "unitCode",
                line.quantity().unit().value(),
                line.quantity().xmlValue());
        amount(xml, "cbc:LineExtensionAmount", line.netAmount());
        xml.start("cac:Item");
        xml.element("cbc:Name", line.itemName());
        xml.start("cac:ClassifiedTaxCategory");
        xml.element("cbc:ID", line.vatCategory().code().xmlValue());
        xml.element("cbc:Percent", line.vatCategory().rateXmlValue());
        taxScheme(xml);
        xml.end("cac:ClassifiedTaxCategory");
        xml.end("cac:Item");
        xml.start("cac:Price");
        xml.element(
                "cbc:PriceAmount",
                "currencyID",
                line.unitPrice().currency().value(),
                line.unitPrice().amountXmlValue());
        xml.element(
                "cbc:BaseQuantity",
                "unitCode",
                line.quantity().unit().value(),
                line.unitPrice().baseQuantityXmlValue());
        xml.end("cac:Price");
        xml.end("cac:InvoiceLine");
    }

    private static void amount(Utf8XmlOutput xml, String element, Money money) throws IOException {
        xml.element(element, "currencyID", money.currency().value(), money.xmlValue());
    }

    private static void taxScheme(Utf8XmlOutput xml) throws IOException {
        xml.start("cac:TaxScheme");
        xml.element("cbc:ID", "VAT");
        xml.end("cac:TaxScheme");
    }
}
