package no.beint.brev.benchmark;

import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSetRegistry;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.peppol.PeppolValidation2026_05;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import no.beint.brev.documents.Documents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import peppol.bis.invoice3.api.PeppolBillingApi;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class BillingWriterBenchmarkTest {
    private static ValidationExecutorSetRegistry<IValidationSourceXML> registry;

    @BeforeAll
    static void registerCurrentBillingRules() {
        registry = new ValidationExecutorSetRegistry<>();
        try {
            PeppolValidation2026_05.class.getMethod("initBilling", IValidationExecutorSetRegistry.class)
                    .invoke(null, registry);
        } catch (ReflectiveOperationException exception) {
            PeppolValidation2026_05.init(registry);
        }
    }

    @Test
    void benchmarkInvoicesContainTheSameBusinessValues() throws Exception {
        Document brev = parse(Documents.toByteArray(BillingBenchmarkFixture.createBrevInvoice()));
        Document digipost = parse(PeppolBillingApi.create(BillingBenchmarkFixture.createDigipostInvoice())
                .inputStream().readAllBytes());

        for (String element : List.of(
                "ID",
                "IssueDate",
                "DueDate",
                "DocumentCurrencyCode",
                "BuyerReference",
                "EndpointID",
                "RegistrationName",
                "StreetName",
                "CityName",
                "PostalZone",
                "IdentificationCode",
                "TaxAmount",
                "TaxableAmount",
                "LineExtensionAmount",
                "TaxExclusiveAmount",
                "TaxInclusiveAmount",
                "PayableAmount",
                "InvoicedQuantity",
                "Name",
                "PriceAmount",
                "BaseQuantity",
                "PaymentMeansCode",
                "PaymentID")) {
            assertEquals(values(brev, element), values(digipost, element), element);
        }

        assertValid(brev);
        assertValid(digipost);
    }

    private static void assertValid(Document document) {
        var executorSet = registry.getOfID(PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
        if (executorSet == null) {
            throw new IllegalStateException("current Peppol Billing rules were not registered");
        }
        var result = ValidationExecutionManager.executeValidation(
                IValidityDeterminator.createDefault(),
                executorSet,
                ValidationSourceXML.create(null, document));
        assertFalse(
                result.containsAtLeastOneError(),
                () -> result.getAllErrors().stream()
                        .map(error -> error.getErrorText(Locale.ENGLISH))
                        .collect(Collectors.joining("\n")));
    }

    private static List<String> values(Document document, String localName) throws Exception {
        var expression = XPathFactory.newInstance().newXPath()
                .compile("//*[local-name()='" + localName + "']/text()");
        var nodes = (org.w3c.dom.NodeList) expression.evaluate(document, XPathConstants.NODESET);
        return java.util.stream.IntStream.range(0, nodes.getLength())
                .mapToObj(index -> nodes.item(index).getNodeValue())
                .toList();
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }
}
