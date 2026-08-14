package no.beint.brev.billing;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PeppolBillingWriterTest {
    @Test
    void writesCompactUtf8UblWithoutAnIntermediateDocumentTree() throws Exception {
        byte[] bytes = PeppolBillingWriter.toByteArray(InvoiceFixtures.invoice());
        String xml = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Invoice"));
        assertTrue(xml.contains("<cbc:Name>Brev &amp; Sønner AS</cbc:Name>"));
        assertTrue(xml.contains("<cbc:Name>Rådgivning &lt;senior&gt;</cbc:Name>"));
        assertTrue(xml.contains("<cbc:Name>Rapport 📄</cbc:Name>"));
        assertTrue(xml.contains("<cbc:TaxAmount currencyID=\"NOK\">625.00</cbc:TaxAmount>"));
        assertTrue(xml.contains("<cbc:PayableAmount currencyID=\"NOK\">3525.50</cbc:PayableAmount>"));
        assertFalse(xml.contains("\n"));

        Document document = parseSecurely(bytes);
        assertEquals("Invoice", document.getDocumentElement().getLocalName());
        assertEquals("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
                document.getDocumentElement().getNamespaceURI());
        assertEquals(2, document.getElementsByTagNameNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
                "InvoiceLine").getLength());
    }

    @Test
    void rejectsCharactersForbiddenByXml10() {
        Invoice invoice = InvoiceFixtures.invoice();
        Party invalidSeller = Party.withVat(
                invoice.seller().endpoint(),
                "Bad\u0000Name",
                invoice.seller().registrationId(),
                invoice.seller().vatIdentifier().orElseThrow(),
                invoice.seller().address());
        Invoice invalid = new Invoice(
                invoice.id(),
                invoice.issueDate(),
                invoice.dueDate(),
                invoice.currency(),
                invoice.buyerReference(),
                invalidSeller,
                invoice.buyer(),
                invoice.payment(),
                invoice.lines());

        assertThrows(IllegalArgumentException.class, () -> PeppolBillingWriter.toByteArray(invalid));
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
