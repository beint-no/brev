package no.beint.brev.benchmark;

import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSetRegistry;
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
import no.beint.brev.documents.AdditionalDocument;
import no.beint.brev.documents.BillingDocument;
import no.beint.brev.documents.BillingLine;
import no.beint.brev.documents.Documents;
import no.beint.brev.documents.Money;
import no.beint.brev.documents.OrderReference;
import no.beint.brev.documents.Party;
import no.beint.brev.documents.PaymentInstruction;
import no.beint.brev.documents.PostalAddress;
import no.beint.brev.documents.Quantity;
import no.beint.brev.documents.TaxSubtotal;
import no.beint.brev.documents.UnitPrice;
import no.beint.brev.documents.VatCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import peppol.bis.invoice3.api.PeppolBillingApi;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class DigipostCompatibilityTest {
    private static final CurrencyCode NOK = new CurrencyCode("NOK");
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 8, 17);
    private static final VatSpec STANDARD = new VatSpec("S", new BigDecimal("25"), null);
    private static final VatSpec ZERO = new VatSpec("Z", BigDecimal.ZERO, null);
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
    void fullReaiInvoiceIsEquivalentAndCurrentProfileValid() throws Exception {
        Scenario scenario = new Scenario(
                false,
                "INV-2026-42",
                new BigDecimal("175.00"),
                List.of(
                        new LineSpec(
                                "1", "Consulting", new BigDecimal("3"), new BigDecimal("33.33"),
                                new BigDecimal("99.99"), new BigDecimal("1.67"), new BigDecimal("25.00"), STANDARD),
                        new LineSpec(
                                "2", "Zero-rated service", BigDecimal.ONE, new BigDecimal("50"),
                                new BigDecimal("50.00"), null, BigDecimal.ZERO, ZERO)),
                List.of(
                        new AttachmentSpec(
                                "invoice-pdf.pdf", "application/pdf", "Invoice-INV-2026-42.pdf",
                                "%PDF-1.4 parity".getBytes(StandardCharsets.US_ASCII)),
                        new AttachmentSpec(
                                "order-attachment-7", "application/pdf", "purchase-order.pdf",
                                "%PDF-1.4 order".getBytes(StandardCharsets.US_ASCII))),
                null,
                true);

        assertEquivalentAndValid(scenario);
    }

    @Test
    void creditNoteWithBillingReferenceIsEquivalentAndCurrentProfileValid() throws Exception {
        Scenario scenario = new Scenario(
                true,
                "CN-2026-7",
                new BigDecimal("125.00"),
                List.of(new LineSpec(
                        "1", "Credited consulting", BigDecimal.ONE, new BigDecimal("100"),
                        new BigDecimal("100.00"), null, new BigDecimal("25.00"), STANDARD)),
                List.of(new AttachmentSpec(
                        "creditnote-pdf.pdf", "application/pdf", "CreditNote-CN-2026-7.pdf",
                        "%PDF-1.4 credit".getBytes(StandardCharsets.US_ASCII))),
                "INV-2026-42",
                true);

        assertEquivalentAndValid(scenario);
    }

    @TestFactory
    Stream<DynamicTest> everyReaiVatCategoryIsEquivalentAndCurrentProfileValid() {
        return Stream.of(
                        STANDARD,
                        ZERO,
                        new VatSpec("E", BigDecimal.ZERO, "Exempt from VAT"),
                        new VatSpec("G", BigDecimal.ZERO, "Export outside the EU"),
                        new VatSpec("AE", BigDecimal.ZERO, "Reverse charge"),
                        new VatSpec("O", null, "Not subject to VAT"))
                .map(vat -> DynamicTest.dynamicTest(vat.code(), () -> assertEquivalentAndValid(new Scenario(
                        false,
                        "VAT-" + vat.code(),
                        STANDARD.equals(vat) ? new BigDecimal("125.00") : new BigDecimal("100.00"),
                        List.of(new LineSpec(
                                "1", "VAT category " + vat.code(), BigDecimal.ONE, new BigDecimal("100"),
                                new BigDecimal("100.00"), null,
                                STANDARD.equals(vat) ? new BigDecimal("25.00") : BigDecimal.ZERO, vat)),
                        List.of(),
                        null,
                        !"O".equals(vat.code())))));
    }

    private static void assertEquivalentAndValid(Scenario scenario) throws Exception {
        Document brev = parse(Documents.toByteArray(createBrev(scenario)));
        Document digipost = parse(createDigipost(scenario));
        assertEquals(semanticLeaves(digipost), semanticLeaves(brev));

        DVRCoordinate validationId = scenario.creditNote()
                ? PeppolValidation2026_05.VID_OPENPEPPOL_CREDIT_NOTE_UBL_V3
                : PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3;
        assertValid(brev, validationId);
        assertValid(digipost, validationId);
    }

    private static BillingDocument createBrev(Scenario scenario) {
        BillingDocument.Builder builder = (scenario.creditNote()
                        ? BillingDocument.creditNote()
                        : BillingDocument.invoice())
                .id(scenario.id())
                .issueDate(ISSUE_DATE)
                .currency(NOK)
                .buyerReference("buyer@example.no")
                .orderReference(OrderReference.salesOrder("SO-42"))
                .seller(brevSeller(scenario.sellerVatRegistered()))
                .buyer(brevBuyer())
                .payment(new PaymentInstruction("NO9386011117947", scenario.id(), "DNBANOKK"))
                .payableAmount(Money.of(NOK, scenario.payableAmount()));
        if (!scenario.creditNote()) {
            builder.dueDate(LocalDate.of(2026, 9, 1));
        }
        if (scenario.referencedInvoiceId() != null) {
            builder.referencedInvoiceId(scenario.referencedInvoiceId());
        }
        scenario.attachments().forEach(attachment -> builder.additionalDocument(new AdditionalDocument(
                attachment.id(), attachment.mimeType(), attachment.fileName(), attachment.content())));
        scenario.lines().forEach(line -> {
            BillingLine billingLine = new BillingLine(
                    line.id(),
                    line.name(),
                    new Quantity(line.quantity(), UnitCode.EA),
                    new UnitPrice(NOK, line.unitPrice()),
                    line.vat().brev(),
                    Money.of(NOK, line.netAmount()));
            if (line.allowance() != null) {
                billingLine = billingLine.withPriceAllowance(Money.of(NOK, line.allowance()));
            }
            builder.line(billingLine);
        });
        builder.taxSubtotals(brevTaxSubtotals(scenario.lines()));
        return builder.build();
    }

    private static byte[] createDigipost(Scenario scenario) throws Exception {
        peppol.bis.invoice3.domain.AccountingSupplierParty seller =
                new peppol.bis.invoice3.domain.AccountingSupplierParty(
                        digipostParty("922989451", "Seller AS", scenario.sellerVatRegistered(), true));
        peppol.bis.invoice3.domain.AccountingCustomerParty buyer =
                new peppol.bis.invoice3.domain.AccountingCustomerParty(
                        digipostParty("987654325", "Buyer AS", false, false));
        peppol.bis.invoice3.domain.TaxTotal taxTotal = digipostTaxTotal(scenario.lines());
        peppol.bis.invoice3.domain.LegalMonetaryTotal totals = digipostTotals(scenario);
        peppol.bis.invoice3.domain.PaymentMeans payment = digipostPayment(scenario.id());
        peppol.bis.invoice3.domain.OrderReference orderReference =
                new peppol.bis.invoice3.domain.OrderReference("NA").withsalesOrderID("SO-42");

        if (scenario.creditNote()) {
            List<peppol.bis.invoice3.domain.CreditNoteLine> lines = scenario.lines().stream()
                    .map(DigipostCompatibilityTest::digipostCreditNoteLine)
                    .toList();
            peppol.bis.invoice3.domain.CreditNote document = new peppol.bis.invoice3.domain.CreditNote(
                            scenario.id(), ISSUE_DATE.toString(), "NOK", seller, buyer, taxTotal, totals, lines)
                    .withBuyerReference("buyer@example.no")
                    .withOrderReference(orderReference)
                    .withPaymentMeans(payment);
            if (scenario.referencedInvoiceId() != null) {
                document.withBillingReference(new peppol.bis.invoice3.domain.BillingReference(
                        new peppol.bis.invoice3.domain.InvoiceDocumentReference(scenario.referencedInvoiceId())));
            }
            scenario.attachments().forEach(attachment ->
                    document.withAdditionalDocumentReferences(digipostAttachment(attachment)));
            return PeppolBillingApi.create(document).inputStream().readAllBytes();
        }

        List<peppol.bis.invoice3.domain.InvoiceLine> lines = scenario.lines().stream()
                .map(DigipostCompatibilityTest::digipostInvoiceLine)
                .toList();
        peppol.bis.invoice3.domain.Invoice document = new peppol.bis.invoice3.domain.Invoice(
                        scenario.id(), ISSUE_DATE.toString(), "NOK", seller, buyer, taxTotal, totals, lines)
                .withDueDate("2026-09-01")
                .withBuyerReference("buyer@example.no")
                .withOrderReference(orderReference)
                .withPaymentMeans(payment);
        scenario.attachments().forEach(attachment ->
                document.withAdditionalDocumentReferences(digipostAttachment(attachment)));
        return PeppolBillingApi.create(document).inputStream().readAllBytes();
    }

    private static Party brevSeller(boolean vatRegistered) {
        Party seller = vatRegistered
                ? Party.withVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                        "Seller AS",
                        "922989451",
                        "NO922989451MVA",
                        brevAddress())
                : Party.withoutVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                        "Seller AS",
                        "922989451",
                        brevAddress());
        return seller.withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION).withForetaksregisteret();
    }

    private static Party brevBuyer() {
        return Party.withoutVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "987654325"),
                        "Buyer AS",
                        "987654325",
                        brevAddress())
                .withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION);
    }

    private static PostalAddress brevAddress() {
        return new PostalAddress(
                "Testveien 1",
                "Floor 2",
                "Oslo",
                "0150",
                new CountryCode("NO"));
    }

    private static peppol.bis.invoice3.domain.Party digipostParty(
            String organizationNumber, String name, boolean vatRegistered, boolean foretaksregisteret) {
        peppol.bis.invoice3.domain.PostalAddress address = new peppol.bis.invoice3.domain.PostalAddress(
                        new peppol.bis.invoice3.domain.Country("NO"))
                .withStreetName("Testveien 1")
                .withAdditionalStreetName("Floor 2")
                .withCityName("Oslo")
                .withPostalZone("0150");
        peppol.bis.invoice3.domain.Party party = new peppol.bis.invoice3.domain.Party(
                        new peppol.bis.invoice3.domain.EndpointID(organizationNumber).withSchemeID("0192"),
                        address,
                        new peppol.bis.invoice3.domain.PartyLegalEntity(name)
                                .withCompanyID(new peppol.bis.invoice3.domain.CompanyID(organizationNumber)
                                        .withSchemeID("0192")))
                .withPartyName(new peppol.bis.invoice3.domain.PartyName(name));
        if (vatRegistered) {
            party.withPartyTaxScheme(new peppol.bis.invoice3.domain.PartyTaxScheme(
                    "NO" + organizationNumber + "MVA", new peppol.bis.invoice3.domain.TaxScheme("VAT")));
        }
        if (foretaksregisteret) {
            party.withPartyTaxScheme(new peppol.bis.invoice3.domain.PartyTaxScheme(
                    "Foretaksregisteret", new peppol.bis.invoice3.domain.TaxScheme("TAX")));
        }
        return party;
    }

    private static List<TaxSubtotal> brevTaxSubtotals(List<LineSpec> lines) {
        Map<VatSpec, List<LineSpec>> groups = lines.stream()
                .collect(Collectors.groupingBy(LineSpec::vat, LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream()
                .map(entry -> new TaxSubtotal(
                        entry.getKey().brev(),
                        Money.of(NOK, sum(entry.getValue(), LineSpec::netAmount)),
                        Money.of(NOK, sum(entry.getValue(), LineSpec::taxAmount))))
                .toList();
    }

    private static peppol.bis.invoice3.domain.TaxTotal digipostTaxTotal(List<LineSpec> lines) {
        peppol.bis.invoice3.domain.TaxTotal total = new peppol.bis.invoice3.domain.TaxTotal(
                new peppol.bis.invoice3.domain.TaxAmount(money(sum(lines, LineSpec::taxAmount)), "NOK"));
        Map<VatSpec, List<LineSpec>> groups = lines.stream()
                .collect(Collectors.groupingBy(LineSpec::vat, LinkedHashMap::new, Collectors.toList()));
        groups.forEach((vat, groupedLines) -> total.withTaxSubtotal(new peppol.bis.invoice3.domain.TaxSubtotal(
                new peppol.bis.invoice3.domain.TaxableAmount(money(sum(groupedLines, LineSpec::netAmount)), "NOK"),
                new peppol.bis.invoice3.domain.TaxAmount(money(sum(groupedLines, LineSpec::taxAmount)), "NOK"),
                vat.digipostTaxCategory())));
        return total;
    }

    private static peppol.bis.invoice3.domain.LegalMonetaryTotal digipostTotals(Scenario scenario) {
        BigDecimal lineExtension = sum(scenario.lines(), LineSpec::netAmount);
        BigDecimal tax = sum(scenario.lines(), LineSpec::taxAmount);
        BigDecimal taxInclusive = lineExtension.add(tax);
        peppol.bis.invoice3.domain.LegalMonetaryTotal totals =
                new peppol.bis.invoice3.domain.LegalMonetaryTotal(
                        new peppol.bis.invoice3.domain.LineExtensionAmount(money(lineExtension), "NOK"),
                        new peppol.bis.invoice3.domain.TaxExclusiveAmount(money(lineExtension), "NOK"),
                        new peppol.bis.invoice3.domain.TaxInclusiveAmount(money(taxInclusive), "NOK"),
                        new peppol.bis.invoice3.domain.PayableAmount(money(scenario.payableAmount()), "NOK"));
        BigDecimal rounding = scenario.payableAmount().subtract(taxInclusive);
        if (rounding.signum() != 0) {
            totals.withPayableRoundingAmount(
                    new peppol.bis.invoice3.domain.PayableRoundingAmount(money(rounding), "NOK"));
        }
        return totals;
    }

    private static peppol.bis.invoice3.domain.InvoiceLine digipostInvoiceLine(LineSpec line) {
        return new peppol.bis.invoice3.domain.InvoiceLine(
                line.id(),
                new peppol.bis.invoice3.domain.InvoicedQuantity(decimal(line.quantity()), "EA"),
                new peppol.bis.invoice3.domain.LineExtensionAmount(money(line.netAmount()), "NOK"),
                digipostItem(line),
                digipostPrice(line));
    }

    private static peppol.bis.invoice3.domain.CreditNoteLine digipostCreditNoteLine(LineSpec line) {
        return new peppol.bis.invoice3.domain.CreditNoteLine(
                line.id(),
                new peppol.bis.invoice3.domain.CreditedQuantity(decimal(line.quantity()), "EA"),
                new peppol.bis.invoice3.domain.LineExtensionAmount(money(line.netAmount()), "NOK"),
                digipostItem(line),
                digipostPrice(line));
    }

    private static peppol.bis.invoice3.domain.Item digipostItem(LineSpec line) {
        return new peppol.bis.invoice3.domain.Item(line.name(), line.vat().digipostClassifiedTaxCategory());
    }

    private static peppol.bis.invoice3.domain.Price digipostPrice(LineSpec line) {
        peppol.bis.invoice3.domain.Price price = new peppol.bis.invoice3.domain.Price(
                        new peppol.bis.invoice3.domain.PriceAmount(decimal(line.unitPrice()), "NOK"))
                .withBaseQuantity(new peppol.bis.invoice3.domain.BaseQuantity("1").withUnitCode("EA"));
        if (line.allowance() != null) {
            price.withAllowanceCharge(new peppol.bis.invoice3.domain.PriceAllowanceCharge(
                    false, new peppol.bis.invoice3.domain.Amount(money(line.allowance()), "NOK")));
        }
        return price;
    }

    private static peppol.bis.invoice3.domain.PaymentMeans digipostPayment(String reference) {
        return new peppol.bis.invoice3.domain.PaymentMeans(
                        new peppol.bis.invoice3.domain.PaymentMeansCode("30").withName("Credit transfer"))
                .withPaymentID(reference)
                .withPayeeFinancialAccount(new peppol.bis.invoice3.domain.PayeeFinancialAccount("NO9386011117947")
                        .withFinancialInstitutionBranch(
                                new peppol.bis.invoice3.domain.FinancialInstitutionBranch("DNBANOKK")));
    }

    private static peppol.bis.invoice3.domain.AdditionalDocumentReference digipostAttachment(
            AttachmentSpec attachment) {
        return new peppol.bis.invoice3.domain.AdditionalDocumentReference(attachment.id())
                .withAttachment(new peppol.bis.invoice3.domain.Attachment().withEmbeddedDocumentBinaryObject(
                        new peppol.bis.invoice3.domain.EmbeddedDocumentBinaryObject(
                                attachment.mimeType(),
                                attachment.fileName(),
                                Base64.getEncoder().encodeToString(attachment.content()))));
    }

    private static void assertValid(Document document, DVRCoordinate validationId) {
        var executorSet = registry.getOfID(validationId);
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

    private static BigDecimal sum(
            List<LineSpec> lines, java.util.function.Function<LineSpec, BigDecimal> value) {
        return lines.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record Scenario(
            boolean creditNote,
            String id,
            BigDecimal payableAmount,
            List<LineSpec> lines,
            List<AttachmentSpec> attachments,
            String referencedInvoiceId,
            boolean sellerVatRegistered) {
    }

    private record LineSpec(
            String id,
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal netAmount,
            BigDecimal allowance,
            BigDecimal taxAmount,
            VatSpec vat) {
    }

    private record AttachmentSpec(String id, String mimeType, String fileName, byte[] content) {
        private AttachmentSpec {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    private record VatSpec(String code, BigDecimal rate, String exemptionReason) {
        private VatCategory brev() {
            return switch (code) {
                case "S" -> VatCategory.standard(rate);
                case "Z" -> VatCategory.zero();
                case "E" -> VatCategory.exempt(exemptionReason);
                case "G" -> VatCategory.export(exemptionReason);
                case "AE" -> VatCategory.reverseCharge(exemptionReason);
                case "O" -> VatCategory.outsideScope(exemptionReason);
                default -> throw new IllegalArgumentException("unsupported VAT category " + code);
            };
        }

        private peppol.bis.invoice3.domain.TaxCategory digipostTaxCategory() {
            peppol.bis.invoice3.domain.TaxCategory category = new peppol.bis.invoice3.domain.TaxCategory(
                    code, new peppol.bis.invoice3.domain.TaxScheme("VAT"));
            if (rate != null) {
                category.withPercent(decimal(rate));
            }
            if (exemptionReason != null) {
                category.withTaxExemptionReason(exemptionReason);
            }
            return category;
        }

        private peppol.bis.invoice3.domain.ClassifiedTaxCategory digipostClassifiedTaxCategory() {
            peppol.bis.invoice3.domain.ClassifiedTaxCategory category =
                    new peppol.bis.invoice3.domain.ClassifiedTaxCategory(
                            code, new peppol.bis.invoice3.domain.TaxScheme("VAT"));
            if (rate != null) {
                category.withPercent(decimal(rate));
            }
            return category;
        }
    }
}
