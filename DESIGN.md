# Design rationale

## Objective

Brev should make the common, correct Peppol path the cheapest path at compile time and runtime. It optimizes for:

1. illegal API combinations being unrepresentable where practical;
2. current-profile correctness rather than broad UBL compatibility;
3. one document object graph and one streaming serialization pass;
4. predictable startup, allocation, and artifact size;
5. generated, auditable validation code;
6. explicit external artefact versions and provenance.

## Why a new library

The mature Peppol ecosystem is actively maintained and should remain the interoperability reference. It is nevertheless shaped by requirements Brev rejects.

`ph-ubl` models the complete XML schema. Schema-generated types can ensure that a value is placed in an element accepted by UBL, but cannot make Peppol Billing cardinality, process compatibility, code-list membership, or cross-field business rules compile-time properties.

The Digipost Billing generator is closer to the desired semantic model, but its mutable builders accept many dates, amounts, identifiers, and codes as strings. Generation constructs an XML tree and ultimately materializes a complete XML string before bytes.

PHIVE correctly prioritizes generic, authoritative runtime validation across many standards and historical rule sets. Its DOM/XSD/Schematron/Saxon architecture is valuable as a reference implementation, but is not the minimum work needed for a typed current-profile document already held in memory.

Phase4, Oxalis, Holodeck B2B, and Domibus solve AS4 transport. Their WS-Security, XML signature, MIME, receipt, retry, and conformance responsibilities are materially different from a Billing document API.

Brev can therefore coexist with and contribute to those projects. Its initial transport adapter will use Phase4.

## Scope boundary

```text
application domain
       │
       ▼
brev-core
       │
       ├── brev-documents          (Billing write + read)
       │         │
       │         └── conformance   (test-only PHIVE/Saxon)
       │
       ├── brev-smp                (types now, JDK client later)
       │
       └── brev-ap                 (types now, Phase4 adapter later)
                 │
            Peppol network
```

All of the above live in this repository. Consumers depend on one artifact.
There is no `ph-commons` equivalent that every module must drag in.

The document layers must not depend on transport. The transport SPI must not leak Phase4 types.

## Model strategy

Brev models EN 16931/Peppol business terms, not JAXB elements. UBL is an output and input syntax.

- Records and final immutable classes are the default.
- Distinct identifiers have distinct types even when their wire representation is a string.
- Mandatory values are constructor parameters.
- Collections are non-null, non-empty where required, and defensively copied.
- Current finite code lists become enums or generated closed types.
- Open or frequently changing code lists use validated value objects generated from release data.
- Derived totals have a single source of truth.
- Profile-specific variants use sealed types when their required fields differ.

Compile-time types cannot prove data obtained from a database or network is semantically valid. Boundary parsing and cross-field validation remain explicit runtime operations.

## Output strategy

The writer emits fixed UBL element order directly to an `OutputStream` or channel:

```text
immutable document → buffered UTF-8 encoder → destination
```

It does not create DOM nodes, JAXB wrappers, reflection metadata, an intermediate character document, or a second byte-array copy unless the caller asks for `toByteArray()`.

Static markup is emitted as ASCII. Dynamic values are validated as XML 1.0 characters, escaped, and encoded directly into the writer buffer. Attachments will use incremental Base64 encoding.

Pretty printing is deliberately absent from the production writer. Diagnostic formatting belongs in tooling.

## Input strategy

The reader will be a bounded event parser rather than a general XML object mapper. It will:

- disable DTDs and all external entity/schema access;
- enforce depth, element-count, text-size, and attachment-size limits;
- recognize current UBL paths at compile-generated states;
- skip unknown extension content under an explicit policy;
- construct one immutable document graph;
- optionally project selected fields without constructing a full document.

Structural recognition of an archived document does not imply validation support for its obsolete release.

## Validation strategy

Official Schematron and code-list artefacts remain the source of truth. Brev will compile them during its own build; consumers will not run an XPath engine.

```text
official artefacts + pinned checksums
            │
            ▼
    parse and normalize rules
            │
            ▼
 typed rule IR + unsupported-expression failure
            │
            ▼
       generated Java
            │
            ▼
 differential corpus against PHIVE/Saxon
```

No rule is silently ignored. If a new release uses unsupported XPath semantics, Brev's release build fails. Each diagnostic preserves the official rule ID, severity, test expression, artefact version, and applicable document location.

See [docs/validation.md](docs/validation.md).

## Version policy

The core contains one target release and, temporarily, may contain the next published release behind an explicit preview API. It does not accumulate deprecated validators or compatibility overloads.

The stable business model is not renamed for every Peppol micro release. Release-specific rule data, code lists, identifiers, and generated validators are versioned separately. A semantic breaking change may intentionally break the Java API.

See [docs/version-policy.md](docs/version-policy.md).

## Performance contract

Performance claims require reproducible JMH results and allocation profiles. Initial gates are:

- no third-party runtime dependency in `brev-core` or `brev-billing`;
- no DOM, JAXB, reflection, or intermediate complete XML string;
- core plus Billing JARs below 500 KiB before validation artefacts;
- at least 5× generation throughput and 3× lower allocation than the current Digipost path on the agreed ReAI corpus;
- bounded memory when writing attachments and reading hostile input.

The comparison benchmark will live in a separate source set so reference libraries never become transitive dependencies.

## Security and interoperability

Performance never overrides XML correctness, Peppol rule fidelity, or output determinism. Generated XML must pass the official schemas, Schematron artefacts, and an independent validator.

AS4 is not reimplemented in the initial plan. Native transport work requires a separate threat model, conformance suite, testbed evidence, certificate-rotation design, and a demonstrated bottleneck that cannot reasonably be addressed upstream.
