# Modules

Brev is one repository and four independently published artifacts. Take the
smallest module that contains the types you call.

```text
application
    │
    ├── brev-core
    │
    ├── brev-documents     current Billing write / read
    │
    ├── brev-smp           typed lookup results (client later)
    │
    └── brev-ap            typed send/receive (Phase4 adapter later)
```

`brev-documents` depends on `brev-core` only. It does not depend on SMP or
AP. SMP and AP do not depend on documents. Nothing in the published runtime
depends on PHIVE, JAXB, Saxon, or Phase4.

## brev-core

Participant, endpoint, document-type, process, scheme, currency, country, and
unit value types. `PeppolRelease.CURRENT` is the only bundled rule target.

## brev-documents

The first complete vertical: Peppol BIS Billing 3.0.21 invoices and credit
notes. This is the replacement for Digipost + `ph-ubl` in ReAI.

- immutable model
- derived tax totals
- direct UTF-8 writer
- bounded StAX reader
- no DOM or JAXB on the production path

## brev-smp

Types for an SMP lookup result. A JDK HTTP client is a later change to this
module, not a new repository.

## brev-ap

Types for an outbound Peppol message and a send receipt. The first transport
implementation will be a Phase4 adapter behind these types. Native AS4 is a
separate decision.
