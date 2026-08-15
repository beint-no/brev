package no.beint.brev.conformance;

import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
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
import no.beint.brev.documents.UnitPrice;
import no.beint.brev.documents.VatCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class CurrentBillingConformanceTest {
    private static final CurrencyCode NOK = new CurrencyCode("NOK");
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
    void positiveInvoicePassesPeppolBilling3021() throws Exception {
        assertValid(Documents.toByteArray(positiveInvoice()), PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
    }

    @Test
    void creditNotePassesPeppolBilling3021() throws Exception {
        assertValid(Documents.toByteArray(creditNote()), PeppolValidation2026_05.VID_OPENPEPPOL_CREDIT_NOTE_UBL_V3);
    }

    @Test
    void norwegianInvoiceWithAttachmentPasses() throws Exception {
        assertValid(Documents.toByteArray(norwegianInvoice()), PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
    }

    @Test
    void exemptExportAndReverseChargeInvoicesPass() throws Exception {
        assertValid(Documents.toByteArray(singleCategoryInvoice("E-1", VatCategory.exempt("Exempt from VAT"))),
                PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
        assertValid(Documents.toByteArray(singleCategoryInvoice("G-1", VatCategory.export("Export outside the EU"))),
                PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
        assertValid(Documents.toByteArray(singleCategoryInvoice("AE-1", VatCategory.reverseCharge("Reverse charge"))),
                PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
        assertValid(Documents.toByteArray(outsideScopeInvoice()),
                PeppolValidation2026_05.VID_OPENPEPPOL_INVOICE_UBL_V3);
    }

    private static void assertValid(byte[] xml, DVRCoordinate vesId) throws Exception {
        Document document = parseSecurely(xml);
        IValidationExecutorSet<IValidationSourceXML> executorSet = registry.getOfID(vesId);
        if (executorSet == null) {
            throw new IllegalStateException("validation executor was not registered: " + vesId);
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

    private static BillingDocument positiveInvoice() {
        return baseInvoice("INV-2026-1")
                .line(new BillingLine(
                        "1",
                        "Rådgivning",
                        new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1250")),
                        VatCategory.standard(new BigDecimal("25"))))
                .build();
    }

    private static BillingDocument creditNote() {
        return BillingDocument.creditNote()
                .id("CN-2026-1")
                .issueDate(LocalDate.of(2026, 8, 18))
                .currency(NOK)
                .buyerReference("buyer@example.no")
                .referencedInvoiceId("INV-2026-1")
                .seller(seller())
                .buyer(buyer())
                .payment(new PaymentInstruction("NO9386011117947", "CN-2026-1"))
                .line(new BillingLine(
                        "1",
                        "Kreditert rådgivning",
                        new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1250")),
                        VatCategory.standard(new BigDecimal("25"))))
                .build();
    }

    private static BillingDocument norwegianInvoice() {
        return baseInvoice("INV-NO-1")
                .orderReference(OrderReference.salesOrder("SO-1"))
                .payment(new PaymentInstruction(
                        "NO9386011117947",
                        Optional.of("INV-NO-1"),
                        Optional.of("DNBANOKK")))
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
                        Optional.of(Money.of(NOK, new BigDecimal("50.00")))))
                .build();
    }

    private static BillingDocument singleCategoryInvoice(String id, VatCategory vat) {
        return baseInvoice(id)
                .line(new BillingLine(
                        "1",
                        "Line",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("100")),
                        vat))
                .build();
    }

    private static BillingDocument outsideScopeInvoice() {
        return BillingDocument.invoice()
                .id("O-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("buyer@example.no")
                .seller(Party.withoutVat(
                                new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                                "Brev AS",
                                "922989451",
                                new PostalAddress("Dokumentveien 1", "Oslo", "0150", new CountryCode("NO")))
                        .withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION)
                        .withForetaksregisteret())
                .buyer(buyer())
                .payment(new PaymentInstruction("NO9386011117947", "1234567890123456789012345"))
                .line(new BillingLine(
                        "1",
                        "Line",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(NOK, new BigDecimal("100")),
                        VatCategory.outsideScope("Not subject to VAT")))
                .build();
    }

    private static BillingDocument.Builder baseInvoice(String id) {
        return BillingDocument.invoice()
                .id(id)
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("buyer@example.no")
                .seller(seller())
                .buyer(buyer())
                .payment(new PaymentInstruction("NO9386011117947", "1234567890123456789012345"));
    }

    private static Party seller() {
        return Party.withVat(
                        new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "922989451"),
                        "Brev AS",
                        "922989451",
                        "NO922989451MVA",
                        new PostalAddress("Dokumentveien 1", "Oslo", "0150", new CountryCode("NO")))
                .withRegistrationScheme(SchemeId.NORWEGIAN_ORGANIZATION)
                .withForetaksregisteret();
    }

    private static Party buyer() {
        return Party.withoutVat(
                        new EndpointId(new SchemeId("9922"), "OPTBCNTRLP2001"),
                        "Kjøper AS",
                        "SE4598375937",
                        new PostalAddress("Testgata 2", "Stockholm", "11122", new CountryCode("SE")));
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
