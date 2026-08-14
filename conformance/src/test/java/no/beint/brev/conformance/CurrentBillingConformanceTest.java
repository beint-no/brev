package no.beint.brev.conformance;

import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.peppol.PeppolValidation2026_05;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;
import no.beint.brev.billing.Invoice;
import no.beint.brev.billing.InvoiceLine;
import no.beint.brev.billing.Party;
import no.beint.brev.billing.PaymentInstruction;
import no.beint.brev.billing.PeppolBillingWriter;
import no.beint.brev.billing.PostalAddress;
import no.beint.brev.billing.Quantity;
import no.beint.brev.billing.TaxCategoryCode;
import no.beint.brev.billing.UnitPrice;
import no.beint.brev.billing.VatCategory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class CurrentBillingConformanceTest {
    @Test
    void generatedFixturePassesPeppolBilling3021() throws Exception {
        Document document = parseSecurely(PeppolBillingWriter.toByteArray(invoice()));
        ValidationExecutorSetRegistry<IValidationSourceXML> registry = new ValidationExecutorSetRegistry<>();
        PeppolValidation2026_05.init(registry);
        var executorSet = registry.getOfID(PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
        if (executorSet == null) {
            throw new IllegalStateException("Peppol Billing 3.0.21 validation executor was not registered");
        }
        var result = ValidationExecutionManager.executeValidation(
                IValidityDeterminator.createDefault(),
                executorSet,
                ValidationSourceXML.create(null, document));

        assertFalse(
                result.containsAtLeastOneError(),
                () -> result.getAllErrors().stream()
                        .map(error -> error.getErrorText(Locale.ENGLISH))
                        .toList()
                        .toString());
    }

    private static Invoice invoice() {
        CurrencyCode nok = new CurrencyCode("NOK");
        SchemeId organization = new SchemeId("0192");
        Party seller = Party.withVat(
                new EndpointId(organization, "922989451"),
                "Brev AS",
                "922989451",
                "NO922989451MVA",
                new PostalAddress("Dokumentveien 1", "Oslo", "0150", new CountryCode("NO")));
        Party buyer = Party.withoutVat(
                new EndpointId(new SchemeId("9922"), "OPTBCNTRLP2001"),
                "Kjøper AS",
                "SE4598375937",
                new PostalAddress("Testgata 2", "Stockholm", "11122", new CountryCode("SE")));
        InvoiceLine line = new InvoiceLine(
                "1",
                "Rådgivning",
                new Quantity(new BigDecimal("10"), new UnitCode("HUR")),
                new UnitPrice(nok, new BigDecimal("1250")),
                new VatCategory(TaxCategoryCode.STANDARD_RATE, new BigDecimal("25")));
        return new Invoice(
                "INV-2026-1",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 9, 1),
                nok,
                "buyer@example.no",
                seller,
                buyer,
                new PaymentInstruction("NO9386011117947", "1234567890123456789012345"),
                List.of(line));
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
