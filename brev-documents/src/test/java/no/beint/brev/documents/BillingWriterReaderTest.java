package no.beint.brev.documents;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BillingWriterReaderTest {
    @Test
    void writesCompactUtf8Invoice() throws Exception {
        byte[] bytes = Documents.toByteArray(DocumentFixtures.invoice());
        String xml = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Invoice"));
        assertTrue(xml.contains("<cbc:Name>Brev &amp; Sønner AS</cbc:Name>"));
        assertTrue(xml.contains("<cbc:Name>Rådgivning &lt;senior&gt;</cbc:Name>"));
        assertTrue(xml.contains("<cbc:Name>Rapport 📄</cbc:Name>"));
        assertTrue(xml.contains("<cbc:CompanyID>Foretaksregisteret</cbc:CompanyID>"));
        assertTrue(xml.contains("schemeID=\"0192\""));
        assertTrue(xml.contains("<cbc:SalesOrderID>2026-100</cbc:SalesOrderID>"));
        assertFalse(xml.contains("\n"));

        Document document = parseSecurely(bytes);
        assertEquals("Invoice", document.getDocumentElement().getLocalName());
        assertEquals(2, document.getElementsByTagNameNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
                "InvoiceLine").getLength());
    }

    @Test
    void writesCreditNoteRootAndTypeCode() {
        String xml = new String(Documents.toByteArray(DocumentFixtures.creditNote()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<CreditNote "));
        assertTrue(xml.contains("<cbc:CreditNoteTypeCode>381</cbc:CreditNoteTypeCode>"));
        assertTrue(xml.contains("<cac:CreditNoteLine>"));
        assertTrue(xml.contains("<cbc:CreditedQuantity"));
        assertFalse(xml.contains("<cbc:DueDate"));
        assertTrue(xml.contains("<cbc:ID>INV-2026-1</cbc:ID>"));
    }

    @Test
    void roundTripsNorwegianInvoiceWithAttachmentAndPriceAllowance() {
        BillingDocument original = DocumentFixtures.norwegianInvoiceWithAttachment();
        BillingDocument parsed = Documents.read(Documents.toByteArray(original));

        assertEquals(original.id(), parsed.id());
        assertEquals(original.payableAmount().xmlValue(), parsed.payableAmount().xmlValue());
        assertEquals("DNBANOKK", parsed.payment().orElseThrow().bic().orElseThrow());
        assertEquals(1, parsed.additionalDocuments().size());
        assertArrayEquals(
                original.additionalDocuments().getFirst().content(),
                parsed.additionalDocuments().getFirst().content());
        assertEquals("50.00", parsed.lines().getFirst().priceAllowance().orElseThrow().xmlValue());
        assertTrue(parsed.seller().registeredInForetaksregisteret());
        assertEquals("0192", parsed.seller().registrationScheme().orElseThrow().value());
    }

    @Test
    void writesEveryVatCategoryReAiEmits() {
        String xml = new String(Documents.toByteArray(DocumentFixtures.mixedVatInvoice()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<cbc:ID>S</cbc:ID>"));
        assertTrue(xml.contains("<cbc:ID>E</cbc:ID>"));
        assertTrue(xml.contains("<cbc:ID>G</cbc:ID>"));
        assertTrue(xml.contains("<cbc:ID>AE</cbc:ID>"));
        assertTrue(xml.contains("<cbc:ID>O</cbc:ID>"));
        assertTrue(xml.contains("<cbc:TaxExemptionReason>Reverse charge</cbc:TaxExemptionReason>"));
        assertFalse(xml.contains("<cbc:Percent></cbc:Percent>"));
    }

    @Test
    void rejectsCharactersForbiddenByXml10() {
        assertThrows(IllegalArgumentException.class, () -> Documents.toByteArray(
                BillingDocument.invoice()
                        .id("INV-X")
                        .issueDate(java.time.LocalDate.of(2026, 8, 17))
                        .dueDate(java.time.LocalDate.of(2026, 9, 1))
                        .currency(DocumentFixtures.NOK)
                        .buyerReference("ref")
                        .seller(Party.withVat(
                                DocumentFixtures.seller().endpoint().orElseThrow(),
                                "Bad\u0000Name",
                                "913341464",
                                "NO913341464MVA",
                                DocumentFixtures.seller().address()))
                        .buyer(DocumentFixtures.buyer())
                        .line(DocumentFixtures.invoice().lines().getFirst())
                        .build()));
    }

    @Test
    void readsInboundBookkeepingFieldsUsedByReAi() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                  <cbc:ID>EXT-9</cbc:ID>
                  <cbc:IssueDate>2026-08-01</cbc:IssueDate>
                  <cbc:DueDate>2026-08-15</cbc:DueDate>
                  <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>
                  <cbc:DocumentCurrencyCode>NOK</cbc:DocumentCurrencyCode>
                  <cbc:BuyerReference>ref</cbc:BuyerReference>
                  <cac:AccountingSupplierParty><cac:Party>
                    <cbc:EndpointID schemeID="0192">922989451</cbc:EndpointID>
                    <cac:PartyName><cbc:Name>Ekstern AS</cbc:Name></cac:PartyName>
                    <cac:PostalAddress><cac:Country><cbc:IdentificationCode>NO</cbc:IdentificationCode></cac:Country></cac:PostalAddress>
                    <cac:PartyLegalEntity><cbc:RegistrationName>Ekstern AS</cbc:RegistrationName></cac:PartyLegalEntity>
                  </cac:Party></cac:AccountingSupplierParty>
                  <cac:AccountingCustomerParty><cac:Party>
                    <cbc:EndpointID schemeID="0192">987654321</cbc:EndpointID>
                    <cac:PartyName><cbc:Name>Kjøper</cbc:Name></cac:PartyName>
                    <cac:PostalAddress><cac:Country><cbc:IdentificationCode>NO</cbc:IdentificationCode></cac:Country></cac:PostalAddress>
                    <cac:PartyLegalEntity><cbc:RegistrationName>Kjøper</cbc:RegistrationName></cac:PartyLegalEntity>
                  </cac:Party></cac:AccountingCustomerParty>
                  <cac:PaymentMeans>
                    <cbc:PaymentMeansCode>30</cbc:PaymentMeansCode>
                    <cbc:PaymentID>KID99</cbc:PaymentID>
                    <cac:PayeeFinancialAccount>
                      <cbc:ID>NO9386011117947</cbc:ID>
                      <cac:FinancialInstitutionBranch>
                        <cbc:ID>DNBANOKK</cbc:ID>
                        <cac:FinancialInstitution>
                          <cbc:Name>DNB Bank ASA</cbc:Name>
                          <cac:Address><cac:Country><cbc:IdentificationCode>NO</cbc:IdentificationCode></cac:Country></cac:Address>
                        </cac:FinancialInstitution>
                      </cac:FinancialInstitutionBranch>
                    </cac:PayeeFinancialAccount>
                  </cac:PaymentMeans>
                  <cac:TaxTotal><cbc:TaxAmount currencyID="NOK">0.00</cbc:TaxAmount></cac:TaxTotal>
                  <cac:LegalMonetaryTotal>
                    <cbc:LineExtensionAmount currencyID="NOK">10.00</cbc:LineExtensionAmount>
                    <cbc:TaxExclusiveAmount currencyID="NOK">10.00</cbc:TaxExclusiveAmount>
                    <cbc:TaxInclusiveAmount currencyID="NOK">10.00</cbc:TaxInclusiveAmount>
                    <cbc:PayableAmount currencyID="NOK">10.00</cbc:PayableAmount>
                  </cac:LegalMonetaryTotal>
                  <cac:InvoiceLine>
                    <cbc:ID>1</cbc:ID>
                    <cbc:InvoicedQuantity unitCode="EA">1</cbc:InvoicedQuantity>
                    <cbc:LineExtensionAmount currencyID="NOK">10.00</cbc:LineExtensionAmount>
                    <cac:Item>
                      <cbc:Description>Monthly retainer</cbc:Description>
                      <cbc:Name>Advisory</cbc:Name>
                      <cac:ClassifiedTaxCategory><cbc:ID>Z</cbc:ID><cbc:Percent>0</cbc:Percent>
                        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>
                      </cac:ClassifiedTaxCategory>
                    </cac:Item>
                    <cac:Price><cbc:PriceAmount currencyID="NOK">10</cbc:PriceAmount></cac:Price>
                  </cac:InvoiceLine>
                </Invoice>
                """;
        BillingDocument parsed = Documents.read(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals("Monthly retainer", parsed.lines().getFirst().itemDescription().orElseThrow());
        assertEquals("Advisory", parsed.lines().getFirst().itemName());
        assertEquals("DNB Bank ASA", parsed.payment().orElseThrow().bankName().orElseThrow());
        assertEquals("NO", parsed.payment().orElseThrow().bankCountryCode().orElseThrow());
        assertEquals("KID99", parsed.payment().orElseThrow().paymentReference().orElseThrow());
    }

    @Test
    void skipsUnknownInboundElements() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                  <cbc:CustomizationID>x</cbc:CustomizationID>
                  <cbc:ProfileID>y</cbc:ProfileID>
                  <cbc:ID>IN-1</cbc:ID>
                  <cbc:IssueDate>2026-08-17</cbc:IssueDate>
                  <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>
                  <cbc:DocumentCurrencyCode>NOK</cbc:DocumentCurrencyCode>
                  <cbc:BuyerReference>ref</cbc:BuyerReference>
                  <cac:FutureExtension><cbc:ID>ignore</cbc:ID></cac:FutureExtension>
                  <cac:AccountingSupplierParty><cac:Party>
                    <cbc:EndpointID schemeID="0192">111111111</cbc:EndpointID>
                    <cac:PartyName><cbc:Name>S</cbc:Name></cac:PartyName>
                    <cac:PostalAddress><cac:Country><cbc:IdentificationCode>NO</cbc:IdentificationCode></cac:Country></cac:PostalAddress>
                    <cac:PartyLegalEntity><cbc:RegistrationName>S</cbc:RegistrationName></cac:PartyLegalEntity>
                  </cac:Party></cac:AccountingSupplierParty>
                  <cac:AccountingCustomerParty><cac:Party>
                    <cbc:EndpointID schemeID="0192">222222222</cbc:EndpointID>
                    <cac:PartyName><cbc:Name>B</cbc:Name></cac:PartyName>
                    <cac:PostalAddress><cac:Country><cbc:IdentificationCode>NO</cbc:IdentificationCode></cac:Country></cac:PostalAddress>
                    <cac:PartyLegalEntity><cbc:RegistrationName>B</cbc:RegistrationName></cac:PartyLegalEntity>
                  </cac:Party></cac:AccountingCustomerParty>
                  <cac:TaxTotal><cbc:TaxAmount currencyID="NOK">0.00</cbc:TaxAmount></cac:TaxTotal>
                  <cac:LegalMonetaryTotal>
                    <cbc:LineExtensionAmount currencyID="NOK">10.00</cbc:LineExtensionAmount>
                    <cbc:TaxExclusiveAmount currencyID="NOK">10.00</cbc:TaxExclusiveAmount>
                    <cbc:TaxInclusiveAmount currencyID="NOK">10.00</cbc:TaxInclusiveAmount>
                    <cbc:PayableAmount currencyID="NOK">10.00</cbc:PayableAmount>
                  </cac:LegalMonetaryTotal>
                  <cac:InvoiceLine>
                    <cbc:ID>1</cbc:ID>
                    <cbc:InvoicedQuantity unitCode="EA">1</cbc:InvoicedQuantity>
                    <cbc:LineExtensionAmount currencyID="NOK">10.00</cbc:LineExtensionAmount>
                    <cac:Item><cbc:Name>Widget</cbc:Name>
                      <cac:ClassifiedTaxCategory><cbc:ID>Z</cbc:ID><cbc:Percent>0</cbc:Percent>
                        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>
                      </cac:ClassifiedTaxCategory>
                    </cac:Item>
                    <cac:Price><cbc:PriceAmount currencyID="NOK">10</cbc:PriceAmount></cac:Price>
                  </cac:InvoiceLine>
                </Invoice>
                """;
        BillingDocument parsed = Documents.read(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals("IN-1", parsed.id());
        assertEquals("Widget", parsed.lines().getFirst().itemName());
        assertEquals("10.00", parsed.payableAmount().xmlValue());
    }

    private static Document parseSecurely(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
}
