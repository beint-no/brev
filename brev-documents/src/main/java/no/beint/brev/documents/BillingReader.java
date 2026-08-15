package no.beint.brev.documents;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Bounded StAX reader for current Peppol Billing Invoice and CreditNote documents. */
public final class BillingReader {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_ELEMENTS = 200_000;
    private static final int MAX_TEXT = 2_000_000;
    private static final int MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024;

    private BillingReader() {
    }

    public static BillingDocument read(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return read(new ByteArrayInputStream(bytes));
    }

    public static Optional<BillingDocument> tryRead(byte[] bytes) {
        try {
            return Optional.of(read(bytes));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static BillingDocument read(InputStream input) {
        Objects.requireNonNull(input, "input");
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            try {
                return new Parser(reader).parse();
            } finally {
                reader.close();
            }
        } catch (XMLStreamException exception) {
            throw new IllegalArgumentException("invalid Billing XML", exception);
        }
    }

    private static final class Parser {
        private final XMLStreamReader reader;
        private int depth;
        private int elements;

        private Parser(XMLStreamReader reader) {
            this.reader = reader;
        }

        private BillingDocument parse() throws XMLStreamException {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    countedStart();
                    if ("Invoice".equals(reader.getLocalName())) {
                        return parseDocument(BillingDocumentType.INVOICE);
                    }
                    if ("CreditNote".equals(reader.getLocalName())) {
                        return parseDocument(BillingDocumentType.CREDIT_NOTE);
                    }
                    skipSubtree();
                }
            }
            throw new IllegalArgumentException("document is not a Peppol Billing Invoice or CreditNote");
        }

        private BillingDocument parseDocument(BillingDocumentType type) throws XMLStreamException {
            String id = null;
            LocalDate issueDate = null;
            LocalDate dueDate = null;
            CurrencyCode currency = null;
            String buyerReference = null;
            OrderReference orderReference = null;
            String referencedInvoiceId = null;
            Party seller = null;
            Party buyer = null;
            PaymentInstruction payment = null;
            LocalDate paymentDueDate = null;
            List<AdditionalDocument> attachments = new ArrayList<>();
            List<DocumentAllowanceCharge> allowances = new ArrayList<>();
            List<BillingLine> lines = new ArrayList<>();
            List<TaxSubtotal> taxSubtotals = new ArrayList<>();
            Money taxTotal = null;
            Money lineExtension = null;
            Money taxExclusive = null;
            Money taxInclusive = null;
            Money chargeTotal = null;
            Money payableRounding = null;
            Money payable = null;
            String typeCode = null;

            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> id = text();
                    case "IssueDate" -> issueDate = LocalDate.parse(text());
                    case "DueDate" -> dueDate = LocalDate.parse(text());
                    case "DocumentCurrencyCode" -> currency = new CurrencyCode(text());
                    case "BuyerReference" -> buyerReference = text();
                    case "InvoiceTypeCode", "CreditNoteTypeCode" -> typeCode = text();
                    case "OrderReference" -> orderReference = parseOrderReference();
                    case "BillingReference" ->
                            referencedInvoiceId = firstPresent(referencedInvoiceId, parseBillingReference());
                    case "AdditionalDocumentReference" -> parseAttachment().ifPresent(attachments::add);
                    case "AccountingSupplierParty" -> seller = parseParty();
                    case "AccountingCustomerParty" -> buyer = parseParty();
                    case "PaymentMeans" -> {
                        ParsedPayment parsed = parsePayment();
                        if (payment == null) {
                            payment = parsed.instruction;
                            paymentDueDate = parsed.dueDate;
                        }
                    }
                    case "AllowanceCharge" -> parseAllowance().ifPresent(allowances::add);
                    case "TaxTotal" -> {
                        ParsedTaxTotal parsed = parseTaxTotal();
                        if (taxTotal == null) {
                            taxTotal = parsed.taxAmount;
                            taxSubtotals.addAll(parsed.subtotals);
                        }
                    }
                    case "LegalMonetaryTotal" -> {
                        ParsedTotals totals = parseTotals();
                        lineExtension = totals.lineExtension;
                        taxExclusive = totals.taxExclusive;
                        taxInclusive = totals.taxInclusive;
                        chargeTotal = totals.chargeTotal;
                        payableRounding = totals.payableRounding;
                        payable = totals.payable;
                    }
                    case "InvoiceLine", "CreditNoteLine" -> lines.add(parseLine(currency));
                    default -> skipSubtree();
                }
            }
            if (id == null || issueDate == null || seller == null || buyer == null || lines.isEmpty()) {
                throw new IllegalArgumentException("Billing document is missing required fields");
            }
            CurrencyCode documentCurrency = currency != null ? currency : firstCurrency(lines, taxTotal, payable);
            Money zero = Money.zero(documentCurrency);
            if (dueDate == null) {
                dueDate = paymentDueDate;
            }
            return BillingDocument.parsed(
                    type,
                    id,
                    issueDate,
                    Optional.ofNullable(dueDate),
                    documentCurrency,
                    Optional.ofNullable(buyerReference),
                    Optional.ofNullable(orderReference),
                    Optional.ofNullable(referencedInvoiceId),
                    seller,
                    buyer,
                    Optional.ofNullable(payment),
                    attachments,
                    allowances,
                    lines,
                    firstNonNull(lineExtension, sumNets(lines, documentCurrency)),
                    firstNonNull(taxExclusive, firstNonNull(lineExtension, sumNets(lines, documentCurrency))),
                    firstNonNull(taxInclusive, firstNonNull(payable, zero)),
                    Optional.ofNullable(chargeTotal),
                    Optional.ofNullable(payableRounding),
                    firstNonNull(payable, zero),
                    taxSubtotals,
                    firstNonNull(taxTotal, zero),
                    Optional.ofNullable(typeCode));
        }

        private OrderReference parseOrderReference() throws XMLStreamException {
            String id = null;
            String salesOrderId = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> id = text();
                    case "SalesOrderID" -> salesOrderId = text();
                    default -> skipSubtree();
                }
            }
            if (id == null) {
                throw new IllegalArgumentException("OrderReference is missing ID");
            }
            return new OrderReference(id, Optional.ofNullable(salesOrderId));
        }

        private String parseBillingReference() throws XMLStreamException {
            String invoiceId = null;
            while (nextChild()) {
                if ("InvoiceDocumentReference".equals(reader.getLocalName())) {
                    invoiceId = firstPresent(invoiceId, parseNestedId());
                } else {
                    skipSubtree();
                }
            }
            return invoiceId;
        }

        private String parseNestedId() throws XMLStreamException {
            String id = null;
            while (nextChild()) {
                if ("ID".equals(reader.getLocalName())) {
                    id = text();
                } else {
                    skipSubtree();
                }
            }
            return id;
        }

        private Optional<AdditionalDocument> parseAttachment() throws XMLStreamException {
            String id = "attachment";
            String mimeType = null;
            String fileName = null;
            String uri = null;
            byte[] content = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> id = text();
                    case "Attachment" -> {
                        ParsedBinary binary = parseBinary();
                        mimeType = binary.mimeType;
                        fileName = binary.fileName;
                        uri = binary.uri;
                        content = binary.content;
                    }
                    default -> skipSubtree();
                }
            }
            if (content == null || content.length == 0) {
                return Optional.empty();
            }
            String resolvedName = fileName != null && !fileName.isBlank()
                    ? fileName
                    : (uri != null && !uri.isBlank() ? uri : id);
            return Optional.of(new AdditionalDocument(
                    id,
                    mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType,
                    resolvedName,
                    content,
                    Optional.ofNullable(uri)));
        }

        private ParsedBinary parseBinary() throws XMLStreamException {
            ParsedBinary binary = new ParsedBinary();
            while (nextChild()) {
                if ("EmbeddedDocumentBinaryObject".equals(reader.getLocalName())) {
                    binary.mimeType = attribute("mimeCode");
                    binary.fileName = attribute("filename");
                    binary.uri = attribute("uri");
                    String encoded = text();
                    if (encoded != null && !encoded.isBlank()) {
                        byte[] decoded = Base64.getMimeDecoder().decode(encoded.replace(" ", ""));
                        if (decoded.length > MAX_ATTACHMENT_BYTES) {
                            throw new IllegalArgumentException("embedded document exceeds size limit");
                        }
                        binary.content = decoded;
                    }
                } else {
                    skipSubtree();
                }
            }
            return binary;
        }

        private Party parseParty() throws XMLStreamException {
            Party party = null;
            while (nextChild()) {
                if ("Party".equals(reader.getLocalName())) {
                    party = parsePartyContents();
                } else {
                    skipSubtree();
                }
            }
            if (party == null) {
                throw new IllegalArgumentException("party wrapper is missing cac:Party");
            }
            return party;
        }

        private Party parsePartyContents() throws XMLStreamException {
            EndpointId endpoint = null;
            String legalName = null;
            String partyName = null;
            String registrationId = null;
            SchemeId registrationScheme = null;
            String vatIdentifier = null;
            boolean foretaksregisteret = false;
            PostalAddress address = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "EndpointID" -> endpoint = parseEndpoint();
                    case "PartyName" -> partyName = firstPresent(partyName, parseNestedName());
                    case "PostalAddress" -> address = parseAddress();
                    case "PartyTaxScheme" -> {
                        ParsedTaxScheme scheme = parseTaxScheme();
                        if ("TAX".equals(scheme.scheme) && "Foretaksregisteret".equals(scheme.companyId)) {
                            foretaksregisteret = true;
                        } else if (scheme.companyId != null && vatIdentifier == null) {
                            vatIdentifier = scheme.companyId;
                        }
                    }
                    case "PartyLegalEntity" -> {
                        ParsedLegalEntity entity = parseLegalEntity();
                        legalName = firstPresent(legalName, entity.name);
                        registrationId = firstPresent(registrationId, entity.companyId);
                        if (entity.scheme != null) {
                            registrationScheme = entity.scheme;
                        }
                    }
                    default -> skipSubtree();
                }
            }
            String name = firstPresent(legalName, partyName);
            if (name == null) {
                throw new IllegalArgumentException("party is missing a name");
            }
            if (address == null) {
                address = PostalAddress.ofCountry(new CountryCode("NO"));
            }
            return new Party(
                    Optional.ofNullable(endpoint),
                    name,
                    Optional.ofNullable(registrationId),
                    Optional.ofNullable(registrationScheme),
                    Optional.ofNullable(vatIdentifier),
                    foretaksregisteret,
                    address);
        }

        private EndpointId parseEndpoint() throws XMLStreamException {
            String scheme = attribute("schemeID");
            String value = text();
            if (value == null || value.isBlank()) {
                return null;
            }
            SchemeId schemeId = scheme == null || scheme.isBlank()
                    ? SchemeId.NORWEGIAN_ORGANIZATION
                    : new SchemeId(scheme);
            return new EndpointId(schemeId, value);
        }

        private String parseNestedName() throws XMLStreamException {
            String name = null;
            while (nextChild()) {
                if ("Name".equals(reader.getLocalName()) || "RegistrationName".equals(reader.getLocalName())) {
                    name = text();
                } else {
                    skipSubtree();
                }
            }
            return name;
        }

        private PostalAddress parseAddress() throws XMLStreamException {
            String street = null;
            String additional = null;
            String city = null;
            String postal = null;
            CountryCode country = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "StreetName" -> street = text();
                    case "AdditionalStreetName" -> additional = text();
                    case "CityName" -> city = text();
                    case "PostalZone" -> postal = text();
                    case "Country" -> country = parseCountry();
                    default -> skipSubtree();
                }
            }
            if (country == null) {
                country = new CountryCode("NO");
            }
            return new PostalAddress(
                    Optional.ofNullable(street),
                    Optional.ofNullable(additional),
                    Optional.ofNullable(city),
                    Optional.ofNullable(postal),
                    country);
        }

        private CountryCode parseCountry() throws XMLStreamException {
            CountryCode country = null;
            while (nextChild()) {
                if ("IdentificationCode".equals(reader.getLocalName())) {
                    country = new CountryCode(text());
                } else {
                    skipSubtree();
                }
            }
            return country;
        }

        private ParsedTaxScheme parseTaxScheme() throws XMLStreamException {
            ParsedTaxScheme scheme = new ParsedTaxScheme();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "CompanyID" -> scheme.companyId = text();
                    case "TaxScheme" -> scheme.scheme = parseNestedId();
                    default -> skipSubtree();
                }
            }
            return scheme;
        }

        private ParsedLegalEntity parseLegalEntity() throws XMLStreamException {
            ParsedLegalEntity entity = new ParsedLegalEntity();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "RegistrationName" -> entity.name = text();
                    case "CompanyID" -> {
                        String scheme = attribute("schemeID");
                        entity.companyId = text();
                        if (scheme != null && !scheme.isBlank()) {
                            entity.scheme = new SchemeId(scheme);
                        }
                    }
                    default -> skipSubtree();
                }
            }
            return entity;
        }

        private ParsedPayment parsePayment() throws XMLStreamException {
            String account = null;
            String paymentId = null;
            String bic = null;
            String bankName = null;
            String bankCountry = null;
            LocalDate dueDate = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "PaymentID" -> paymentId = firstPresent(paymentId, text());
                    case "PaymentDueDate" -> dueDate = LocalDate.parse(text());
                    case "PayeeFinancialAccount" -> {
                        ParsedAccount parsed = parseAccount();
                        account = firstPresent(account, parsed.id);
                        bic = firstPresent(bic, parsed.bic);
                        bankName = firstPresent(bankName, parsed.bankName);
                        bankCountry = firstPresent(bankCountry, parsed.bankCountry);
                    }
                    default -> skipSubtree();
                }
            }
            if (account == null) {
                return new ParsedPayment(null, dueDate);
            }
            return new ParsedPayment(
                    new PaymentInstruction(
                            account,
                            Optional.ofNullable(paymentId),
                            Optional.ofNullable(bic),
                            Optional.ofNullable(bankName),
                            Optional.ofNullable(bankCountry)),
                    dueDate);
        }

        private ParsedAccount parseAccount() throws XMLStreamException {
            ParsedAccount account = new ParsedAccount();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> account.id = text();
                    case "FinancialInstitutionBranch" -> parseBranch(account);
                    default -> skipSubtree();
                }
            }
            return account;
        }

        private void parseBranch(ParsedAccount account) throws XMLStreamException {
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> account.bic = firstPresent(account.bic, text());
                    case "Name" -> account.bankName = firstPresent(account.bankName, text());
                    case "FinancialInstitution" -> parseFinancialInstitution(account);
                    default -> skipSubtree();
                }
            }
        }

        private void parseFinancialInstitution(ParsedAccount account) throws XMLStreamException {
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> account.bic = firstPresent(account.bic, text());
                    case "Name" -> account.bankName = firstPresent(account.bankName, text());
                    case "Address" -> {
                        PostalAddress address = parseAddress();
                        account.bankCountry = firstPresent(account.bankCountry, address.country().value());
                    }
                    default -> skipSubtree();
                }
            }
        }

        private Optional<DocumentAllowanceCharge> parseAllowance() throws XMLStreamException {
            Boolean charge = null;
            String reason = null;
            Money amount = null;
            VatCategory vat = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ChargeIndicator" -> charge = Boolean.parseBoolean(text());
                    case "AllowanceChargeReason" -> reason = firstPresent(reason, text());
                    case "Amount" -> amount = parseAmount();
                    case "TaxCategory" -> vat = parseVatCategory();
                    default -> skipSubtree();
                }
            }
            if (amount == null) {
                return Optional.empty();
            }
            return Optional.of(new DocumentAllowanceCharge(
                    Boolean.TRUE.equals(charge),
                    Optional.ofNullable(reason),
                    amount,
                    Optional.ofNullable(vat)));
        }

        private ParsedTaxTotal parseTaxTotal() throws XMLStreamException {
            Money taxAmount = null;
            List<TaxSubtotal> subtotals = new ArrayList<>();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "TaxAmount" -> taxAmount = parseAmount();
                    case "TaxSubtotal" -> parseTaxSubtotal().ifPresent(subtotals::add);
                    default -> skipSubtree();
                }
            }
            return new ParsedTaxTotal(taxAmount, subtotals);
        }

        private Optional<TaxSubtotal> parseTaxSubtotal() throws XMLStreamException {
            Money taxable = null;
            Money tax = null;
            VatCategory category = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "TaxableAmount" -> taxable = parseAmount();
                    case "TaxAmount" -> tax = parseAmount();
                    case "TaxCategory" -> category = parseVatCategory();
                    default -> skipSubtree();
                }
            }
            if (taxable == null || tax == null || category == null) {
                return Optional.empty();
            }
            return Optional.of(new TaxSubtotal(category, taxable, tax));
        }

        private ParsedTotals parseTotals() throws XMLStreamException {
            ParsedTotals totals = new ParsedTotals();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "LineExtensionAmount" -> totals.lineExtension = parseAmount();
                    case "TaxExclusiveAmount" -> totals.taxExclusive = parseAmount();
                    case "TaxInclusiveAmount" -> totals.taxInclusive = parseAmount();
                    case "ChargeTotalAmount" -> totals.chargeTotal = parseAmount();
                    case "PayableRoundingAmount" -> totals.payableRounding = parseAmount();
                    case "PayableAmount" -> totals.payable = parseAmount();
                    default -> skipSubtree();
                }
            }
            return totals;
        }

        private BillingLine parseLine(CurrencyCode documentCurrency) throws XMLStreamException {
            String id = "1";
            Quantity quantity = null;
            Money net = null;
            String itemName = "Item";
            String itemDescription = null;
            VatCategory vat = null;
            UnitPrice unitPrice = null;
            Money priceAllowance = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> id = text();
                    case "InvoicedQuantity", "CreditedQuantity" -> quantity = parseQuantity();
                    case "LineExtensionAmount" -> net = parseAmount();
                    case "Item" -> {
                        ParsedItem item = parseItem();
                        itemName = firstPresent(item.name, firstPresent(item.description, itemName));
                        itemDescription = firstPresent(itemDescription, item.description);
                        vat = item.vat;
                    }
                    case "Price" -> {
                        ParsedPrice price = parsePrice();
                        unitPrice = price.unitPrice;
                        priceAllowance = price.allowance;
                    }
                    default -> skipSubtree();
                }
            }
            CurrencyCode currency = net != null
                    ? net.currency()
                    : (documentCurrency != null ? documentCurrency : new CurrencyCode("NOK"));
            if (quantity == null) {
                quantity = new Quantity(BigDecimal.ONE, UnitCode.EA);
            }
            if (net == null) {
                net = Money.zero(currency);
            }
            if (unitPrice == null) {
                unitPrice = new UnitPrice(currency, BigDecimal.ZERO);
            }
            if (vat == null) {
                vat = VatCategory.zero();
            }
            return new BillingLine(
                    id,
                    itemName,
                    quantity,
                    unitPrice,
                    vat,
                    net,
                    Optional.ofNullable(priceAllowance),
                    Optional.ofNullable(itemDescription));
        }

        private ParsedItem parseItem() throws XMLStreamException {
            ParsedItem item = new ParsedItem();
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "Name" -> item.name = text();
                    case "Description" -> item.description = firstPresent(item.description, text());
                    case "ClassifiedTaxCategory" -> item.vat = parseVatCategory();
                    default -> skipSubtree();
                }
            }
            return item;
        }

        private ParsedPrice parsePrice() throws XMLStreamException {
            ParsedPrice price = new ParsedPrice();
            BigDecimal amount = null;
            CurrencyCode currency = null;
            BigDecimal base = BigDecimal.ONE;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "PriceAmount" -> {
                        currency = currencyAttribute();
                        amount = new BigDecimal(text());
                    }
                    case "BaseQuantity" -> {
                        String value = text();
                        if (value != null && !value.isBlank()) {
                            base = new BigDecimal(value);
                        }
                    }
                    case "AllowanceCharge" -> price.allowance = parsePriceAllowance();
                    default -> skipSubtree();
                }
            }
            if (amount != null && currency != null) {
                price.unitPrice = new UnitPrice(currency, amount, base.signum() > 0 ? base : BigDecimal.ONE);
            }
            return price;
        }

        private Money parsePriceAllowance() throws XMLStreamException {
            Money amount = null;
            while (nextChild()) {
                if ("Amount".equals(reader.getLocalName())) {
                    amount = parseAmount();
                } else {
                    skipSubtree();
                }
            }
            return amount;
        }

        private VatCategory parseVatCategory() throws XMLStreamException {
            String code = null;
            BigDecimal rate = null;
            String reason = null;
            while (nextChild()) {
                switch (reader.getLocalName()) {
                    case "ID" -> code = text();
                    case "Percent" -> {
                        String value = text();
                        if (value != null && !value.isBlank()) {
                            rate = new BigDecimal(value);
                        }
                    }
                    case "TaxExemptionReason" -> reason = text();
                    default -> skipSubtree();
                }
            }
            Optional<TaxCategoryCode> typed = TaxCategoryCode.fromXmlValue(code);
            if (typed.isPresent()) {
                return switch (typed.orElseThrow()) {
                    case STANDARD_RATE -> rate == null || rate.signum() == 0
                            ? VatCategory.zero()
                            : VatCategory.standard(rate);
                    case ZERO_RATE -> VatCategory.zero();
                    case EXEMPT -> VatCategory.exempt(firstPresent(reason, "Exempt from VAT"));
                    case EXPORT -> VatCategory.export(firstPresent(reason, "Export outside the EU"));
                    case REVERSE_CHARGE -> VatCategory.reverseCharge(firstPresent(reason, "Reverse charge"));
                    case OUTSIDE_SCOPE -> VatCategory.outsideScope(firstPresent(reason, "Not subject to VAT"));
                };
            }
            if (rate != null && rate.signum() > 0) {
                return VatCategory.standard(rate);
            }
            return VatCategory.exempt(firstPresent(reason, "Exempt from VAT"));
        }

        private Quantity parseQuantity() throws XMLStreamException {
            String unit = attribute("unitCode");
            String value = text();
            UnitCode unitCode = unit == null || unit.isBlank() ? UnitCode.EA : new UnitCode(unit);
            return new Quantity(value == null || value.isBlank() ? BigDecimal.ONE : new BigDecimal(value), unitCode);
        }

        private Money parseAmount() throws XMLStreamException {
            CurrencyCode currency = currencyAttribute();
            return new Money(currency, new BigDecimal(text()));
        }

        private CurrencyCode currencyAttribute() {
            String currency = attribute("currencyID");
            return new CurrencyCode(currency == null || currency.isBlank() ? "NOK" : currency);
        }

        /**
         * Advance to the next child START_ELEMENT of the current element.
         * Returns false when the current element's END_ELEMENT is reached.
         */
        private boolean nextChild() throws XMLStreamException {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    countedStart();
                    return true;
                }
                if (event == XMLStreamConstants.END_ELEMENT) {
                    return false;
                }
            }
            return false;
        }

        private void countedStart() {
            elements++;
            depth++;
            if (depth > MAX_DEPTH) {
                throw new IllegalArgumentException("XML exceeds depth limit");
            }
            if (elements > MAX_ELEMENTS) {
                throw new IllegalArgumentException("XML exceeds element limit");
            }
        }

        private void skipSubtree() throws XMLStreamException {
            int remaining = 1;
            while (remaining > 0 && reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    countedStart();
                    remaining++;
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    remaining--;
                    depth--;
                }
            }
        }

        private String text() throws XMLStreamException {
            StringBuilder builder = new StringBuilder();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    builder.append(reader.getText());
                    if (builder.length() > MAX_TEXT) {
                        throw new IllegalArgumentException("XML text exceeds size limit");
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                    return builder.toString();
                } else if (event == XMLStreamConstants.START_ELEMENT) {
                    countedStart();
                    skipSubtree();
                }
            }
            return builder.toString();
        }

        private String attribute(String name) {
            String value = reader.getAttributeValue(null, name);
            if (value != null) {
                return value;
            }
            for (int index = 0; index < reader.getAttributeCount(); index++) {
                if (name.equals(reader.getAttributeLocalName(index))) {
                    return reader.getAttributeValue(index);
                }
            }
            return null;
        }
    }

    private static String firstPresent(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static Money firstNonNull(Money first, Money second) {
        return first != null ? first : second;
    }

    private static Money sumNets(List<BillingLine> lines, CurrencyCode currency) {
        return lines.stream().map(BillingLine::netAmount).reduce(Money.zero(currency), Money::add);
    }

    private static CurrencyCode firstCurrency(List<BillingLine> lines, Money taxTotal, Money payable) {
        if (taxTotal != null) {
            return taxTotal.currency();
        }
        if (payable != null) {
            return payable.currency();
        }
        if (!lines.isEmpty()) {
            return lines.getFirst().netAmount().currency();
        }
        return new CurrencyCode("NOK");
    }

    private static final class ParsedBinary {
        private String mimeType;
        private String fileName;
        private String uri;
        private byte[] content;
    }

    private static final class ParsedTaxScheme {
        private String companyId;
        private String scheme;
    }

    private static final class ParsedLegalEntity {
        private String name;
        private String companyId;
        private SchemeId scheme;
    }

    private static final class ParsedPayment {
        private final PaymentInstruction instruction;
        private final LocalDate dueDate;

        private ParsedPayment(PaymentInstruction instruction, LocalDate dueDate) {
            this.instruction = instruction;
            this.dueDate = dueDate;
        }
    }

    private static final class ParsedAccount {
        private String id;
        private String bic;
        private String bankName;
        private String bankCountry;
    }

    private static final class ParsedTaxTotal {
        private final Money taxAmount;
        private final List<TaxSubtotal> subtotals;

        private ParsedTaxTotal(Money taxAmount, List<TaxSubtotal> subtotals) {
            this.taxAmount = taxAmount;
            this.subtotals = subtotals;
        }
    }

    private static final class ParsedTotals {
        private Money lineExtension;
        private Money taxExclusive;
        private Money taxInclusive;
        private Money chargeTotal;
        private Money payableRounding;
        private Money payable;
    }

    private static final class ParsedItem {
        private String name;
        private String description;
        private VatCategory vat;
    }

    private static final class ParsedPrice {
        private UnitPrice unitPrice;
        private Money allowance;
    }
}
