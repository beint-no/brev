package no.beint.brev.documents;

import no.beint.brev.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** An immutable current-profile Peppol Billing invoice or credit note. */
public final class BillingDocument {
    private final BillingDocumentType type;
    private final String id;
    private final LocalDate issueDate;
    private final Optional<LocalDate> dueDate;
    private final CurrencyCode currency;
    private final Optional<String> buyerReference;
    private final Optional<OrderReference> orderReference;
    private final Optional<String> referencedInvoiceId;
    private final Party seller;
    private final Party buyer;
    private final Optional<PaymentInstruction> payment;
    private final List<AdditionalDocument> additionalDocuments;
    private final List<DocumentAllowanceCharge> allowanceCharges;
    private final List<BillingLine> lines;
    private final Money lineExtensionTotal;
    private final Money taxExclusiveAmount;
    private final Money taxInclusiveAmount;
    private final Optional<Money> chargeTotal;
    private final Optional<Money> payableRounding;
    private final Money payableAmount;
    private final List<TaxSubtotal> taxSubtotals;
    private final Money taxTotal;
    private final Optional<String> invoiceTypeCode;

    BillingDocument(
            BillingDocumentType type,
            String id,
            LocalDate issueDate,
            Optional<LocalDate> dueDate,
            CurrencyCode currency,
            Optional<String> buyerReference,
            Optional<OrderReference> orderReference,
            Optional<String> referencedInvoiceId,
            Party seller,
            Party buyer,
            Optional<PaymentInstruction> payment,
            List<AdditionalDocument> additionalDocuments,
            List<DocumentAllowanceCharge> allowanceCharges,
            List<BillingLine> lines,
            Money lineExtensionTotal,
            Money taxExclusiveAmount,
            Money taxInclusiveAmount,
            Optional<Money> chargeTotal,
            Optional<Money> payableRounding,
            Money payableAmount,
            List<TaxSubtotal> taxSubtotals,
            Money taxTotal,
            Optional<String> invoiceTypeCode) {
        this.type = type;
        this.id = id;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.currency = currency;
        this.buyerReference = buyerReference;
        this.orderReference = orderReference;
        this.referencedInvoiceId = referencedInvoiceId;
        this.seller = seller;
        this.buyer = buyer;
        this.payment = payment;
        this.additionalDocuments = additionalDocuments;
        this.allowanceCharges = allowanceCharges;
        this.lines = lines;
        this.lineExtensionTotal = lineExtensionTotal;
        this.taxExclusiveAmount = taxExclusiveAmount;
        this.taxInclusiveAmount = taxInclusiveAmount;
        this.chargeTotal = chargeTotal;
        this.payableRounding = payableRounding;
        this.payableAmount = payableAmount;
        this.taxSubtotals = taxSubtotals;
        this.taxTotal = taxTotal;
        this.invoiceTypeCode = invoiceTypeCode;
    }

    static BillingDocument parsed(
            BillingDocumentType type,
            String id,
            LocalDate issueDate,
            Optional<LocalDate> dueDate,
            CurrencyCode currency,
            Optional<String> buyerReference,
            Optional<OrderReference> orderReference,
            Optional<String> referencedInvoiceId,
            Party seller,
            Party buyer,
            Optional<PaymentInstruction> payment,
            List<AdditionalDocument> additionalDocuments,
            List<DocumentAllowanceCharge> allowanceCharges,
            List<BillingLine> lines,
            Money lineExtensionTotal,
            Money taxExclusiveAmount,
            Money taxInclusiveAmount,
            Optional<Money> chargeTotal,
            Optional<Money> payableRounding,
            Money payableAmount,
            List<TaxSubtotal> taxSubtotals,
            Money taxTotal,
            Optional<String> invoiceTypeCode) {
        return new BillingDocument(
                type,
                BillingValues.nonBlank(id, "document ID", 100),
                Objects.requireNonNull(issueDate, "issueDate"),
                Objects.requireNonNull(dueDate, "dueDate"),
                Objects.requireNonNull(currency, "currency"),
                Objects.requireNonNull(buyerReference, "buyerReference"),
                Objects.requireNonNull(orderReference, "orderReference"),
                Objects.requireNonNull(referencedInvoiceId, "referencedInvoiceId"),
                Objects.requireNonNull(seller, "seller"),
                Objects.requireNonNull(buyer, "buyer"),
                Objects.requireNonNull(payment, "payment"),
                List.copyOf(additionalDocuments),
                List.copyOf(allowanceCharges),
                List.copyOf(lines),
                Objects.requireNonNull(lineExtensionTotal, "lineExtensionTotal"),
                Objects.requireNonNull(taxExclusiveAmount, "taxExclusiveAmount"),
                Objects.requireNonNull(taxInclusiveAmount, "taxInclusiveAmount"),
                Objects.requireNonNull(chargeTotal, "chargeTotal"),
                Objects.requireNonNull(payableRounding, "payableRounding"),
                Objects.requireNonNull(payableAmount, "payableAmount"),
                List.copyOf(taxSubtotals),
                Objects.requireNonNull(taxTotal, "taxTotal"),
                Objects.requireNonNull(invoiceTypeCode, "invoiceTypeCode"));
    }

    public static Builder invoice() {
        return new Builder(BillingDocumentType.INVOICE);
    }

    public static Builder creditNote() {
        return new Builder(BillingDocumentType.CREDIT_NOTE);
    }

    public BillingDocumentType type() {
        return type;
    }

    public String id() {
        return id;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public Optional<LocalDate> dueDate() {
        return dueDate;
    }

    public CurrencyCode currency() {
        return currency;
    }

    public Optional<String> buyerReference() {
        return buyerReference;
    }

    public Optional<OrderReference> orderReference() {
        return orderReference;
    }

    public Optional<String> referencedInvoiceId() {
        return referencedInvoiceId;
    }

    public Party seller() {
        return seller;
    }

    public Party buyer() {
        return buyer;
    }

    public Optional<PaymentInstruction> payment() {
        return payment;
    }

    public List<AdditionalDocument> additionalDocuments() {
        return additionalDocuments;
    }

    public List<DocumentAllowanceCharge> allowanceCharges() {
        return allowanceCharges;
    }

    public List<BillingLine> lines() {
        return lines;
    }

    public Money lineExtensionTotal() {
        return lineExtensionTotal;
    }

    public Money taxExclusiveAmount() {
        return taxExclusiveAmount;
    }

    public Money taxInclusiveAmount() {
        return taxInclusiveAmount;
    }

    public Optional<Money> chargeTotal() {
        return chargeTotal;
    }

    public Optional<Money> payableRounding() {
        return payableRounding;
    }

    public Money payableAmount() {
        return payableAmount;
    }

    public List<TaxSubtotal> taxSubtotals() {
        return taxSubtotals;
    }

    public Money taxTotal() {
        return taxTotal;
    }

    public Optional<String> invoiceTypeCode() {
        return invoiceTypeCode;
    }

    public boolean isCreditNote() {
        return type.isCreditNote();
    }

    static List<TaxSubtotal> deriveTaxSubtotals(List<BillingLine> lines, CurrencyCode currency) {
        record Key(TaxCategoryCode code, Optional<BigDecimal> rate, Optional<String> reason) {
        }
        Map<Key, Money> taxableByCategory = new LinkedHashMap<>();
        Map<Key, VatCategory> categories = new LinkedHashMap<>();
        for (BillingLine line : lines) {
            VatCategory category = line.vatCategory();
            Key key = new Key(category.code(), category.rate(), category.exemptionReason());
            taxableByCategory.merge(key, line.netAmount(), Money::add);
            categories.putIfAbsent(key, category);
        }
        List<TaxSubtotal> subtotals = new ArrayList<>(taxableByCategory.size());
        taxableByCategory.forEach((key, taxable) -> {
            VatCategory category = categories.get(key);
            BigDecimal tax = category.rate()
                    .map(rate -> taxable.amount()
                            .multiply(rate)
                            .movePointLeft(2)
                            .setScale(2, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO.setScale(2));
            subtotals.add(new TaxSubtotal(category, taxable, new Money(currency, tax)));
        });
        return List.copyOf(subtotals);
    }

    public static final class Builder {
        private final BillingDocumentType type;
        private String id;
        private LocalDate issueDate;
        private Optional<LocalDate> dueDate = Optional.empty();
        private CurrencyCode currency;
        private Optional<String> buyerReference = Optional.empty();
        private Optional<OrderReference> orderReference = Optional.empty();
        private Optional<String> referencedInvoiceId = Optional.empty();
        private Party seller;
        private Party buyer;
        private Optional<PaymentInstruction> payment = Optional.empty();
        private final List<AdditionalDocument> additionalDocuments = new ArrayList<>();
        private final List<BillingLine> lines = new ArrayList<>();

        private Builder(BillingDocumentType type) {
            this.type = type;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = Optional.ofNullable(dueDate);
            return this;
        }

        public Builder currency(CurrencyCode currency) {
            this.currency = currency;
            return this;
        }

        public Builder buyerReference(String buyerReference) {
            this.buyerReference = BillingValues.optionalText(buyerReference, "buyer reference", 200);
            return this;
        }

        public Builder orderReference(OrderReference orderReference) {
            this.orderReference = Optional.ofNullable(orderReference);
            return this;
        }

        public Builder referencedInvoiceId(String referencedInvoiceId) {
            this.referencedInvoiceId = BillingValues.optionalText(referencedInvoiceId, "referenced invoice ID", 100);
            return this;
        }

        public Builder seller(Party seller) {
            this.seller = seller;
            return this;
        }

        public Builder buyer(Party buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder payment(PaymentInstruction payment) {
            this.payment = Optional.ofNullable(payment);
            return this;
        }

        public Builder additionalDocument(AdditionalDocument document) {
            this.additionalDocuments.add(Objects.requireNonNull(document, "additional document"));
            return this;
        }

        public Builder line(BillingLine line) {
            this.lines.add(Objects.requireNonNull(line, "line"));
            return this;
        }

        public BillingDocument build() {
            String documentId = BillingValues.nonBlank(id, "document ID", 100);
            LocalDate issued = Objects.requireNonNull(issueDate, "issueDate");
            CurrencyCode documentCurrency = Objects.requireNonNull(currency, "currency");
            Party sellingParty = Objects.requireNonNull(seller, "seller");
            Party buyingParty = Objects.requireNonNull(buyer, "buyer");
            if (sellingParty.endpoint().isEmpty() || buyingParty.endpoint().isEmpty()) {
                throw new IllegalArgumentException("seller and buyer must have endpoint identifiers");
            }
            if (buyerReference.isEmpty() && orderReference.isEmpty()) {
                throw new IllegalArgumentException("document must have a buyer reference or an order reference");
            }
            dueDate.ifPresent(date -> {
                if (date.isBefore(issued)) {
                    throw new IllegalArgumentException("due date must not precede issue date");
                }
            });
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("document must contain at least one line");
            }
            List<BillingLine> copiedLines = List.copyOf(lines);
            long distinctIds = copiedLines.stream().map(BillingLine::id).distinct().count();
            if (distinctIds != copiedLines.size()) {
                throw new IllegalArgumentException("line IDs must be unique");
            }
            for (BillingLine line : copiedLines) {
                if (!line.unitPrice().currency().equals(documentCurrency)) {
                    throw new IllegalArgumentException(
                            "line " + line.id() + " does not use document currency " + documentCurrency);
                }
            }
            Money lineExtension = copiedLines.stream()
                    .map(BillingLine::netAmount)
                    .reduce(Money.zero(documentCurrency), Money::add);
            List<TaxSubtotal> subtotals = deriveTaxSubtotals(copiedLines, documentCurrency);
            Money tax = subtotals.stream()
                    .map(TaxSubtotal::taxAmount)
                    .reduce(Money.zero(documentCurrency), Money::add);
            Money payable = lineExtension.add(tax);
            return new BillingDocument(
                    type,
                    documentId,
                    issued,
                    dueDate,
                    documentCurrency,
                    buyerReference,
                    orderReference,
                    referencedInvoiceId,
                    sellingParty,
                    buyingParty,
                    payment,
                    List.copyOf(additionalDocuments),
                    List.of(),
                    copiedLines,
                    lineExtension,
                    lineExtension,
                    payable,
                    Optional.empty(),
                    Optional.empty(),
                    payable,
                    subtotals,
                    tax,
                    Optional.of(type.typeCode()));
        }
    }
}
