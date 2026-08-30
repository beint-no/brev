# Brev

**Current Peppol, as one modular JDK 26 library.**

Brev is a dependency-free JVM library for constructing, writing, reading, and
later transporting current Peppol business documents. It lives in **one
repository**. Applications take only the module they need.

The name is Norwegian for “letter.”

> [!WARNING]
> `0.1.1` replaces ReAI's Digipost billing writer. ReAI still uses `ph-ubl` for
> inbound documents, while `brev-smp` and `brev-ap` publish types only. Keep
> official PHIVE validation on the send path until generated-rule parity exists.

## Why one repo, not ten

The Helger stack is the interoperability reference. It is also split across
many repositories because it preserves every UBL version, every Peppol
profile, and years of API compatibility.

Brev occupies the opposite corner:

- one repo, independently published modules;
- current Peppol only;
- JDK 26 baseline, no older-Java tax;
- typed documents instead of the full UBL schema;
- zero third-party runtime dependencies in every published module.

| Module | Take it when you need | Status |
|---|---|---|
| `brev-core` | identifiers, codes, release metadata | shipping |
| `brev-documents` | Billing invoice/credit note model, writer, reader | shipping |
| `brev-smp` | typed SMP lookup results | types only |
| `brev-ap` | typed send/receive messages | types only |

See [docs/modules.md](docs/modules.md), [DESIGN.md](DESIGN.md), and
[ROADMAP.md](ROADMAP.md).

## Current document target

| Component | Target |
|---|---|
| Peppol BIS Billing | 3.0.21 |
| EN 16931 validation artefacts | 1.3.16 |
| Mandatory from | 2026-08-17 |
| UBL syntax | Invoice and CreditNote 2.1 |
| Process | Billing profile 01 |

Historical rule sets are not bundled. When Billing 4 is mandatory, Brev will
replace this writer rather than keep a compatibility flag.

## Example

```java
var invoice = BillingDocument.invoice()
        .id("INV-1")
        .issueDate(LocalDate.of(2026, 8, 17))
        .dueDate(LocalDate.of(2026, 9, 1))
        .currency(new CurrencyCode("NOK"))
        .buyerReference("buyer-reference")
        .seller(seller)
        .buyer(buyer)
        .payment(new PaymentInstruction("NO9386011117947", "payment-reference"))
        .line(new BillingLine(
                "1",
                "Consulting",
                new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                new UnitPrice(new CurrencyCode("NOK"), new BigDecimal("1250")),
                VatCategory.standard(new BigDecimal("25"))))
        .build();

Documents.write(invoice, outputStream);
BillingDocument parsed = Documents.read(Documents.toByteArray(invoice));
```

The writer emits compact XML straight to the supplied stream and does not close
it. Attachments are Base64-encoded incrementally.

## What ReAI can generate today

- invoices and credit notes
- VAT categories S, Z, E, G, AE, O
- Norwegian supplier identity (0192, VAT, Foretaksregisteret)
- order reference, billing reference, IBAN/BIC
- embedded PDF and other attachments
- price-level allowances
- optional payment, due date, and address parts

Unsupported Peppol Billing features fail through the absence of an API.

## Building

Brev requires JDK 26.

```shell
./gradlew clean build
./gradlew :benchmark:jmh
```

The build fails if a published module acquires a third-party runtime
dependency. Conformance runs official PHIVE/Saxon Billing 3.0.21 artefacts as
a test-only oracle.

## License

Apache License 2.0. Imported standards and validation artefacts retain their
own licenses and provenance.
