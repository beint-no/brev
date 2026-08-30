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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import peppol.bis.invoice3.api.PeppolBillingApi;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
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
        assertEquals(semanticLeaves(digipost), semanticLeaves(brev));
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

    private static List<String> semanticLeaves(Document document) {
        List<String> leaves = new ArrayList<>();
        collectLeaves(document.getDocumentElement(), "", leaves);
        return leaves;
    }

    private static void collectLeaves(Element element, String parentPath, List<String> leaves) {
        String path = parentPath + "/{" + element.getNamespaceURI() + "}" + element.getLocalName();
        List<Element> children = new ArrayList<>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                children.add(childElement);
            }
        }
        if (!children.isEmpty()) {
            children.forEach(child -> collectLeaves(child, path, leaves));
            return;
        }
        List<String> attributes = new ArrayList<>();
        for (int index = 0; index < element.getAttributes().getLength(); index++) {
            Node attribute = element.getAttributes().item(index);
            if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                attributes.add("{" + attribute.getNamespaceURI() + "}" + attribute.getLocalName()
                        + "=" + attribute.getNodeValue());
            }
        }
        attributes.sort(Comparator.naturalOrder());
        leaves.add(path + attributes + "=" + element.getTextContent().strip());
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
