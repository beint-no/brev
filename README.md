# Brev

**Current Peppol documents, compiled into small and explicit Java.**

Brev is an experimental, dependency-free JVM library for constructing, writing, reading, and validating current Peppol business documents. It deliberately does not preserve compatibility with obsolete Peppol releases or expose the complete UBL schema as an object model.

The name is Norwegian for “letter.”

> [!WARNING]
> Brev is an early vertical slice, not yet a conformant replacement for a production Peppol Billing generator or validator. The current code writes a deliberately narrow positive-invoice subset. Authoritative validation remains required before transmission.

## Why Brev

Existing JVM libraries solve important but different problems:

- `ph-ubl` provides complete JAXB bindings for several UBL versions;
- the Digipost generator offers a convenient mutable Billing-domain API;
- PHIVE executes authoritative XSD and Schematron validation artefacts;
- Phase4 and Oxalis implement full AS4 access-point transport.

Brev targets the unoccupied space between an application's domain model and transport:

- current Peppol profile semantics rather than every UBL element;
- distinct immutable Java types instead of interchangeable strings;
- required data at construction time;
- calculated totals rather than duplicated mutable values;
- direct buffered UTF-8 output without DOM, JAXB, reflection, or an intermediate XML string;
- validation rules compiled from exact official artefacts into ordinary Java;
- zero third-party runtime dependencies in the document core.

See [DESIGN.md](DESIGN.md) for the architectural rationale, [ROADMAP.md](ROADMAP.md) for the complete implementation plan, and [docs/performance.md](docs/performance.md) for the measured starting point.

## Current target

The repository tracks one release target:

| Component | Target |
|---|---|
| Peppol BIS Billing | 3.0.21 |
| EN 16931 validation artefacts | 1.3.16 |
| Publication date | 2026-05-20 |
| Mandatory from | 2026-08-17 |
| UBL syntax | UBL 2.1 Invoice |
| Process | Billing profile 01 |

Brev does not bundle historical rule sets. See [docs/version-policy.md](docs/version-policy.md).

## Modules

- `brev-core`: participant, endpoint, document, process, currency, country, and unit value types plus release metadata.
- `brev-billing`: immutable Billing model, derived totals, and direct UBL writer.
- `conformance`: test-only PHIVE/Saxon reference validation; never a production dependency.
- `benchmark`: JMH performance harness; never a production dependency.

Planned modules are added only after their conformance gates pass. They include validation, streaming input, SBDH, discovery, reporting, and a Phase4 transport adapter.

## Example

```java
var nok = new CurrencyCode("NOK");
var organization = new SchemeId("0192");
var address = new PostalAddress(
        "Dokumentveien 1", "Oslo", "0150", new CountryCode("NO"));

var seller = Party.withVat(
        new EndpointId(organization, "913341464"),
        "Seller AS", "913341464", "NO913341464MVA", address);
var buyer = Party.withoutVat(
        new EndpointId(organization, "987654321"),
        "Buyer AS", "987654321", address);

var line = new InvoiceLine(
        "1",
        "Consulting",
        new Quantity(new BigDecimal("10"), new UnitCode("HUR")),
        new UnitPrice(nok, new BigDecimal("1250")),
        new VatCategory(TaxCategoryCode.STANDARD_RATE, new BigDecimal("25")));

var invoice = new Invoice(
        "INV-1",
        LocalDate.of(2026, 8, 17),
        LocalDate.of(2026, 9, 1),
        nok,
        "buyer-reference",
        seller,
        buyer,
        new PaymentInstruction("NO9386011117947", "payment-reference"),
        List.of(line));

PeppolBillingWriter.write(invoice, outputStream);
```

The writer emits compact XML straight to the supplied stream and does not close it.

## Deliberately unsupported in the first slice

- Credit notes and negative invoices.
- Allowances, charges, rounding adjustments, prepayments, and multiple payment means.
- Exempt, reverse-charge, intra-community, export, and out-of-scope VAT categories.
- Attachments and document references.
- Full current-profile validation.
- XML parsing, SBDH, SMP, reporting, and AS4.
- Older Peppol Billing releases.

Unsupported features fail through the absence of an API rather than silently producing approximate XML.

## Building

Brev requires JDK 26.

```shell
./gradlew clean build
./gradlew :benchmark:jmh
```

The build also runs the PHIVE/Saxon Billing 3.0.21 conformance fixture and fails if either published module acquires a third-party runtime dependency.

## Status

Version `0.1.0-SNAPSHOT` establishes the API direction and provides an executable performance baseline. It must not be used to transmit invoices until the Phase 1 conformance gate in [ROADMAP.md](ROADMAP.md) is complete.

## License

Apache License 2.0. Imported standards and validation artefacts retain their own licenses and provenance.
