package no.beint.brev.documents;

import no.beint.brev.documents.internal.Utf8XmlOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Writes a Billing document directly as compact UTF-8 UBL 2.1 XML. */
public final class BillingWriter {
    private static final String CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    private BillingWriter() {
    }

    public static void write(BillingDocument document, OutputStream destination) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(destination, "destination");
        Utf8XmlOutput xml = new Utf8XmlOutput(destination);
        xml.declaration();
        xml.startOpen(document.type().rootElement());
        xml.raw(" xmlns=\"");
        xml.raw(document.type().namespace());
        xml.raw("\" xmlns:cac=\"");
        xml.raw(CAC);
        xml.raw("\" xmlns:cbc=\"");
        xml.raw(CBC);
        xml.raw("\"");
        xml.closeStart();

        xml.element("cbc:CustomizationID", BillingDocumentType.CUSTOMIZATION_ID);
        xml.element("cbc:ProfileID", BillingDocumentType.PROCESS.value());
        xml.element("cbc:ID", document.id());
        xml.element("cbc:IssueDate", document.issueDate().toString());
        if (!document.isCreditNote()) {
            xml.optionalElement("cbc:DueDate", document.dueDate().map(Object::toString));
            xml.element("cbc:InvoiceTypeCode", document.type().typeCode());
        } else {
            xml.element("cbc:CreditNoteTypeCode", document.type().typeCode());
        }
        xml.element("cbc:DocumentCurrencyCode", document.currency().value());
        xml.optionalElement("cbc:BuyerReference", document.buyerReference());
        writeOrderReference(xml, document);
        writeBillingReference(xml, document);
        writeAdditionalDocuments(xml, document);
        writeParty(xml, "cac:AccountingSupplierParty", document.seller());
        writeParty(xml, "cac:AccountingCustomerParty", document.buyer());
        writePayment(xml, document);
        writeTaxes(xml, document);
        writeTotals(xml, document);
        for (BillingLine line : document.lines()) {
            writeLine(xml, document, line);
        }
        xml.end(document.type().rootElement());
        xml.finish();
    }

    public static byte[] toByteArray(BillingDocument document) {
        ByteArrayOutputStream destination = new ByteArrayOutputStream(4096);
        try {
            write(document, destination);
        } catch (IOException exception) {
            throw new UncheckedIOException("unexpected in-memory write failure", exception);
        }
        return destination.toByteArray();
    }

    private static void writeOrderReference(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        if (document.orderReference().isEmpty()) {
            return;
        }
        OrderReference reference = document.orderReference().orElseThrow();
        xml.start("cac:OrderReference");
        xml.element("cbc:ID", reference.id());
        xml.optionalElement("cbc:SalesOrderID", reference.salesOrderId());
        xml.end("cac:OrderReference");
    }

    private static void writeBillingReference(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        if (document.referencedInvoiceId().isEmpty()) {
            return;
        }
        xml.start("cac:BillingReference");
        xml.start("cac:InvoiceDocumentReference");
        xml.element("cbc:ID", document.referencedInvoiceId().orElseThrow());
        xml.end("cac:InvoiceDocumentReference");
        xml.end("cac:BillingReference");
    }

    private static void writeAdditionalDocuments(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        for (AdditionalDocument attachment : document.additionalDocuments()) {
            xml.start("cac:AdditionalDocumentReference");
            xml.element("cbc:ID", attachment.id());
            xml.start("cac:Attachment");
            xml.startOpen("cbc:EmbeddedDocumentBinaryObject");
            xml.attribute("mimeCode", attachment.mimeType());
            xml.attribute("filename", attachment.fileName());
            xml.closeStart();
            xml.base64(attachment.content());
            xml.end("cbc:EmbeddedDocumentBinaryObject");
            xml.end("cac:Attachment");
            xml.end("cac:AdditionalDocumentReference");
        }
    }

    private static void writeParty(Utf8XmlOutput xml, String wrapper, Party party) throws IOException {
        xml.start(wrapper);
        xml.start("cac:Party");
        if (party.endpoint().isPresent()) {
            xml.element(
                    "cbc:EndpointID",
                    "schemeID",
                    party.endpoint().orElseThrow().scheme().value(),
                    party.endpoint().orElseThrow().value());
        }
        xml.start("cac:PartyName");
        xml.element("cbc:Name", party.legalName());
        xml.end("cac:PartyName");
        writeAddress(xml, party.address());
        if (party.vatIdentifier().isPresent()) {
            xml.start("cac:PartyTaxScheme");
            xml.element("cbc:CompanyID", party.vatIdentifier().orElseThrow());
            taxScheme(xml, "VAT");
            xml.end("cac:PartyTaxScheme");
        }
        if (party.registeredInForetaksregisteret()) {
            xml.start("cac:PartyTaxScheme");
            xml.element("cbc:CompanyID", "Foretaksregisteret");
            taxScheme(xml, "TAX");
            xml.end("cac:PartyTaxScheme");
        }
        xml.start("cac:PartyLegalEntity");
        xml.element("cbc:RegistrationName", party.legalName());
        if (party.registrationId().isPresent()) {
            if (party.registrationScheme().isPresent()) {
                xml.element(
                        "cbc:CompanyID",
                        "schemeID",
                        party.registrationScheme().orElseThrow().value(),
                        party.registrationId().orElseThrow());
            } else {
                xml.element("cbc:CompanyID", party.registrationId().orElseThrow());
            }
        }
        xml.end("cac:PartyLegalEntity");
        xml.end("cac:Party");
        xml.end(wrapper);
    }

    private static void writeAddress(Utf8XmlOutput xml, PostalAddress address) throws IOException {
        xml.start("cac:PostalAddress");
        xml.optionalElement("cbc:StreetName", address.streetName());
        xml.optionalElement("cbc:AdditionalStreetName", address.additionalStreetName());
        xml.optionalElement("cbc:CityName", address.cityName());
        xml.optionalElement("cbc:PostalZone", address.postalZone());
        xml.start("cac:Country");
        xml.element("cbc:IdentificationCode", address.country().value());
        xml.end("cac:Country");
        xml.end("cac:PostalAddress");
    }

    private static void writePayment(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        if (document.payment().isEmpty()) {
            return;
        }
        PaymentInstruction payment = document.payment().orElseThrow();
        xml.start("cac:PaymentMeans");
        xml.element("cbc:PaymentMeansCode", "name", "Credit transfer", "30");
        xml.optionalElement("cbc:PaymentID", payment.paymentReference());
        xml.start("cac:PayeeFinancialAccount");
        xml.element("cbc:ID", payment.accountId());
        if (payment.bic().isPresent()) {
            xml.start("cac:FinancialInstitutionBranch");
            xml.element("cbc:ID", payment.bic().orElseThrow());
            xml.end("cac:FinancialInstitutionBranch");
        }
        xml.end("cac:PayeeFinancialAccount");
        xml.end("cac:PaymentMeans");
    }

    private static void writeTaxes(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        xml.start("cac:TaxTotal");
        amount(xml, "cbc:TaxAmount", document.taxTotal());
        for (TaxSubtotal subtotal : document.taxSubtotals()) {
            xml.start("cac:TaxSubtotal");
            amount(xml, "cbc:TaxableAmount", subtotal.taxableAmount());
            amount(xml, "cbc:TaxAmount", subtotal.taxAmount());
            writeTaxCategory(xml, "cac:TaxCategory", subtotal.category(), true);
            xml.end("cac:TaxSubtotal");
        }
        xml.end("cac:TaxTotal");
    }

    private static void writeTaxCategory(
            Utf8XmlOutput xml, String name, VatCategory category, boolean includeExemptionReason) throws IOException {
        xml.start(name);
        xml.element("cbc:ID", category.code().xmlValue());
        if (category.rate().isPresent()) {
            xml.element("cbc:Percent", category.rateXmlValue());
        }
        if (includeExemptionReason) {
            xml.optionalElement("cbc:TaxExemptionReason", category.exemptionReason());
        }
        taxScheme(xml, "VAT");
        xml.end(name);
    }

    private static void writeTotals(Utf8XmlOutput xml, BillingDocument document) throws IOException {
        xml.start("cac:LegalMonetaryTotal");
        amount(xml, "cbc:LineExtensionAmount", document.lineExtensionTotal());
        amount(xml, "cbc:TaxExclusiveAmount", document.taxExclusiveAmount());
        amount(xml, "cbc:TaxInclusiveAmount", document.taxInclusiveAmount());
        document.chargeTotal().ifPresent(charge -> {
            try {
                amount(xml, "cbc:ChargeTotalAmount", charge);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        document.payableRounding().ifPresent(rounding -> {
            try {
                amount(xml, "cbc:PayableRoundingAmount", rounding);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        amount(xml, "cbc:PayableAmount", document.payableAmount());
        xml.end("cac:LegalMonetaryTotal");
    }

    private static void writeLine(Utf8XmlOutput xml, BillingDocument document, BillingLine line) throws IOException {
        xml.start("cac:" + document.type().lineElement());
        xml.element("cbc:ID", line.id());
        xml.element(
                "cbc:" + document.type().quantityElement(),
                "unitCode",
                line.quantity().unit().value(),
                line.quantity().xmlValue());
        amount(xml, "cbc:LineExtensionAmount", line.netAmount());
        xml.start("cac:Item");
        xml.optionalElement("cbc:Description", line.itemDescription());
        xml.element("cbc:Name", line.itemName());
        writeTaxCategory(xml, "cac:ClassifiedTaxCategory", line.vatCategory(), false);
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
        if (line.priceAllowance().isPresent()) {
            xml.start("cac:AllowanceCharge");
            xml.element("cbc:ChargeIndicator", "false");
            amount(xml, "cbc:Amount", line.priceAllowance().orElseThrow());
            xml.end("cac:AllowanceCharge");
        }
        xml.end("cac:Price");
        xml.end("cac:" + document.type().lineElement());
    }

    private static void amount(Utf8XmlOutput xml, String element, Money money) throws IOException {
        xml.element(element, "currencyID", money.currency().value(), money.xmlValue());
    }

    private static void taxScheme(Utf8XmlOutput xml, String id) throws IOException {
        xml.start("cac:TaxScheme");
        xml.element("cbc:ID", id);
        xml.end("cac:TaxScheme");
    }
}
