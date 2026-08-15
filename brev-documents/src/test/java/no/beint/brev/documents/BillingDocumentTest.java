package no.beint.brev.documents;

import no.beint.brev.CurrencyCode;
import no.beint.brev.UnitCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BillingDocumentTest {
    @Test
    void derivesTotalsAndTaxBreakdownsOnce() {
        BillingDocument invoice = DocumentFixtures.invoice();

        assertEquals("2900.50", invoice.lineExtensionTotal().xmlValue());
        assertEquals("625.00", invoice.taxTotal().xmlValue());
        assertEquals("3525.50", invoice.payableAmount().xmlValue());
        assertEquals(2, invoice.taxSubtotals().size());
        assertEquals("380", invoice.invoiceTypeCode().orElseThrow());
    }

    @Test
    void creditNoteUsesType381AndOptionalDueDate() {
        BillingDocument credit = DocumentFixtures.creditNote();

        assertTrue(credit.isCreditNote());
        assertEquals("381", credit.invoiceTypeCode().orElseThrow());
        assertTrue(credit.dueDate().isEmpty());
        assertEquals("INV-2026-1", credit.referencedInvoiceId().orElseThrow());
        assertEquals("3125.00", credit.payableAmount().xmlValue());
    }

    @Test
    void rejectsMissingBuyerAndOrderReference() {
        assertThrows(IllegalArgumentException.class, () -> BillingDocument.invoice()
                .id("INV-X")
                .issueDate(LocalDate.of(2026, 8, 17))
                .currency(DocumentFixtures.NOK)
                .seller(DocumentFixtures.seller())
                .buyer(DocumentFixtures.buyer())
                .line(new BillingLine(
                        "1",
                        "X",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(DocumentFixtures.NOK, BigDecimal.TEN),
                        VatCategory.standard(new BigDecimal("25"))))
                .build());
    }

    @Test
    void rejectsCurrencyMismatch() {
        assertThrows(IllegalArgumentException.class, () -> BillingDocument.invoice()
                .id("INV-X")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(DocumentFixtures.NOK)
                .buyerReference("ref")
                .seller(DocumentFixtures.seller())
                .buyer(DocumentFixtures.buyer())
                .line(new BillingLine(
                        "1",
                        "EUR",
                        new Quantity(BigDecimal.ONE, UnitCode.EA),
                        new UnitPrice(new CurrencyCode("EUR"), BigDecimal.TEN),
                        VatCategory.standard(new BigDecimal("25"))))
                .build());
    }
}
