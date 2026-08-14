package no.beint.brev.billing;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable positive Peppol Billing invoice for the initial Brev profile.
 * Totals and tax breakdowns are derived once during construction.
 */
public final class Invoice {
    private final String id;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final CurrencyCode currency;
    private final String buyerReference;
    private final Party seller;
    private final Party buyer;
    private final PaymentInstruction payment;
    private final List<InvoiceLine> lines;
    private final Money lineExtensionTotal;
    private final List<TaxSubtotal> taxSubtotals;
    private final Money taxTotal;
    private final Money payableAmount;

    public Invoice(
            String id,
            LocalDate issueDate,
            LocalDate dueDate,
            CurrencyCode currency,
            String buyerReference,
            Party seller,
            Party buyer,
            PaymentInstruction payment,
            List<InvoiceLine> lines) {
        this.id = BillingValues.nonBlank(id, "invoice ID", 100);
        this.issueDate = Objects.requireNonNull(issueDate, "issueDate");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("due date must not precede issue date");
        }
        this.currency = Objects.requireNonNull(currency, "currency");
        this.buyerReference = BillingValues.nonBlank(buyerReference, "buyer reference", 200);
        this.seller = Objects.requireNonNull(seller, "seller");
        this.buyer = Objects.requireNonNull(buyer, "buyer");
        this.payment = Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("invoice must contain at least one line");
        }
        this.lines = List.copyOf(lines);
        ensureUniqueLineIds(this.lines);
        ensureDocumentCurrency(this.lines, currency);

        this.lineExtensionTotal = calculateLineExtensionTotal(this.lines, currency);
        this.taxSubtotals = calculateTaxSubtotals(this.lines, currency);
        this.taxTotal = taxSubtotals.stream()
                .map(TaxSubtotal::taxAmount)
                .reduce(Money.zero(currency), Money::add);
        this.payableAmount = lineExtensionTotal.add(taxTotal);
    }

    private static void ensureUniqueLineIds(List<InvoiceLine> lines) {
        long distinctIds = lines.stream().map(InvoiceLine::id).distinct().count();
        if (distinctIds != lines.size()) {
            throw new IllegalArgumentException("invoice line IDs must be unique");
        }
    }

    private static void ensureDocumentCurrency(List<InvoiceLine> lines, CurrencyCode currency) {
        for (InvoiceLine line : lines) {
            if (!line.unitPrice().currency().equals(currency)) {
                throw new IllegalArgumentException("line " + line.id() + " does not use document currency " + currency);
            }
        }
    }

    private static Money calculateLineExtensionTotal(List<InvoiceLine> lines, CurrencyCode currency) {
        Money total = Money.zero(currency);
        for (InvoiceLine line : lines) {
            total = total.add(line.netAmount());
        }
        return total;
    }

    private static List<TaxSubtotal> calculateTaxSubtotals(List<InvoiceLine> lines, CurrencyCode currency) {
        Map<VatCategory, Money> taxableByCategory = new LinkedHashMap<>();
        for (InvoiceLine line : lines) {
            taxableByCategory.merge(line.vatCategory(), line.netAmount(), Money::add);
        }
        List<TaxSubtotal> subtotals = new ArrayList<>(taxableByCategory.size());
        taxableByCategory.forEach((category, taxable) -> {
            BigDecimal tax = taxable.amount()
                    .multiply(category.rate())
                    .movePointLeft(2)
                    .setScale(2, RoundingMode.HALF_UP);
            subtotals.add(new TaxSubtotal(category, taxable, new Money(currency, tax)));
        });
        return List.copyOf(subtotals);
    }

    public String id() {
        return id;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public CurrencyCode currency() {
        return currency;
    }

    public String buyerReference() {
        return buyerReference;
    }

    public Party seller() {
        return seller;
    }

    public Party buyer() {
        return buyer;
    }

    public PaymentInstruction payment() {
        return payment;
    }

    public List<InvoiceLine> lines() {
        return lines;
    }

    public Money lineExtensionTotal() {
        return lineExtensionTotal;
    }

    public List<TaxSubtotal> taxSubtotals() {
        return taxSubtotals;
    }

    public Money taxTotal() {
        return taxTotal;
    }

    public Money payableAmount() {
        return payableAmount;
    }
}
