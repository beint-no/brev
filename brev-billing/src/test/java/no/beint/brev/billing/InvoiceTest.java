package no.beint.brev.billing;

import no.beint.brev.CurrencyCode;
import no.beint.brev.UnitCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InvoiceTest {
    @Test
    void derivesTotalsAndTaxBreakdownsOnce() {
        Invoice invoice = InvoiceFixtures.invoice();

        assertEquals("2900.50", invoice.lineExtensionTotal().xmlValue());
        assertEquals("625.00", invoice.taxTotal().xmlValue());
        assertEquals("3525.50", invoice.payableAmount().xmlValue());
        assertEquals(2, invoice.taxSubtotals().size());
    }

    @Test
    void defensivelyCopiesLines() {
        Invoice original = InvoiceFixtures.invoice();
        List<InvoiceLine> mutable = new ArrayList<>(original.lines());
        Invoice copy = new Invoice(
                original.id(),
                original.issueDate(),
                original.dueDate(),
                original.currency(),
                original.buyerReference(),
                original.seller(),
                original.buyer(),
                original.payment(),
                mutable);

        mutable.clear();

        assertEquals(2, copy.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> copy.lines().clear());
    }

    @Test
    void rejectsCurrencyMismatchesAndInvalidDates() {
        Invoice valid = InvoiceFixtures.invoice();
        InvoiceLine eurLine = new InvoiceLine(
                "EUR-1",
                "Wrong currency",
                new Quantity(BigDecimal.ONE, UnitCode.EACH),
                new UnitPrice(new CurrencyCode("EUR"), BigDecimal.TEN),
                new VatCategory(TaxCategoryCode.STANDARD_RATE, new BigDecimal("25")));

        assertThrows(IllegalArgumentException.class, () -> new Invoice(
                "INV-X",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 16),
                valid.currency(),
                valid.buyerReference(),
                valid.seller(),
                valid.buyer(),
                valid.payment(),
                List.of(eurLine)));

        assertThrows(IllegalArgumentException.class, () -> new Invoice(
                "INV-X",
                valid.issueDate(),
                valid.dueDate(),
                valid.currency(),
                valid.buyerReference(),
                valid.seller(),
                valid.buyer(),
                valid.payment(),
                List.of(eurLine)));
    }
}
