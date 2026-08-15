package no.beint.brev.documents;

import no.beint.brev.DocumentTypeId;
import no.beint.brev.ProcessId;

/** The two current Peppol BIS Billing 3 document variants ReAI sends and receives. */
public enum BillingDocumentType {
    INVOICE(
            "Invoice",
            "380",
            "InvoiceLine",
            "InvoicedQuantity",
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
            new DocumentTypeId(
                    "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1")),
    CREDIT_NOTE(
            "CreditNote",
            "381",
            "CreditNoteLine",
            "CreditedQuantity",
            "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2",
            new DocumentTypeId(
                    "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"));

    public static final String CUSTOMIZATION_ID =
            "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
    public static final ProcessId PROCESS =
            new ProcessId("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

    private final String rootElement;
    private final String typeCode;
    private final String lineElement;
    private final String quantityElement;
    private final String namespace;
    private final DocumentTypeId documentTypeId;

    BillingDocumentType(
            String rootElement,
            String typeCode,
            String lineElement,
            String quantityElement,
            String namespace,
            DocumentTypeId documentTypeId) {
        this.rootElement = rootElement;
        this.typeCode = typeCode;
        this.lineElement = lineElement;
        this.quantityElement = quantityElement;
        this.namespace = namespace;
        this.documentTypeId = documentTypeId;
    }

    public String rootElement() {
        return rootElement;
    }

    public String typeCode() {
        return typeCode;
    }

    public String lineElement() {
        return lineElement;
    }

    public String quantityElement() {
        return quantityElement;
    }

    public String namespace() {
        return namespace;
    }

    public DocumentTypeId documentTypeId() {
        return documentTypeId;
    }

    public boolean isCreditNote() {
        return this == CREDIT_NOTE;
    }
}
