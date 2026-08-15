# Roadmap

The phases are ordered by risk reduction. A later phase does not begin merely because an earlier API compiles; its exit gate must be demonstrated.

## Phase 0 — executable architecture

Status: in progress in `0.1.0-SNAPSHOT`.

- [x] JDK 26 modular Gradle build.
- [x] Zero-runtime-dependency enforcement.
- [x] Typed participant, endpoint, currency, country, and unit values.
- [x] Exact release metadata for Billing 3.0.21 / validation artefacts 1.3.16.
- [x] Immutable Billing invoice and credit-note model covering ReAI output.
- [x] Derived line, tax, and payable totals.
- [x] Direct buffered UTF-8 UBL writer for Invoice and CreditNote.
- [x] Bounded StAX reader for inbound Invoice and CreditNote.
- [x] One-repo modules for documents, SMP types, and AP types.
- [x] XML escaping, Unicode, immutability, and invariant tests.
- [x] JMH benchmark entry point.
- [x] Independent XSD and Schematron validation of the emitted fixture through PHIVE/Saxon.
- [ ] Baseline benchmark against the ReAI Digipost generator path.

Exit gate: the fixture passes independent current-release validation and benchmark results are recorded with hardware, JDK, commands, throughput, and allocation.

## Phase 1 — ReAI Billing surface

Status: the ReAI-used subset is implemented in `brev-documents`. Remaining Billing terms that ReAI does not emit stay out of the API.

- [x] Credit notes as a document type, not a flag on a mutable invoice.
- [x] VAT categories S, Z, E, G, AE, O with category-specific fields.
- [x] Price allowances, order/billing references, IBAN/BIC, attachments.
- [x] Incremental Base64 for embedded documents.
- [ ] Generate remaining code-list types from the pinned 3.0.21 artefacts.
- [ ] Document-level allowances, prepayments, rounding, delivery, notes.
- [ ] Profile 02 without weakening profile 01 construction rules.

Exit gate: every business term ReAI emits is represented, tested, and independently validated. Unused Billing terms remain absent.

## Phase 2 — validation compiler

- Pin official artefacts with URL, license, size, and SHA-256 manifest.
- Parse Schematron phases, patterns, rules, assertions, namespace declarations, diagnostics, and parameters.
- Normalize the required XPath 2.0 subset into a typed intermediate representation.
- Fail generation on every unsupported expression or implicit coercion.
- Generate readable Java predicates over the Billing model.
- Generate streaming-document collectors for rules that do not require a complete model.
- Preserve official rule IDs, severities, locations, and messages.
- Differential-test valid and invalid official examples against PHIVE/Saxon.
- Add mutation, property, and fuzz testing focused on rule boundaries.
- Publish the parity matrix and unsupported-expression inventory.

Exit gate: 100% result parity on the agreed corpus, no ignored rules, and independent review of the artefact provenance and generator.

## Phase 3 — bounded streaming reader

- Generate a current-profile path automaton from the supported UBL mapping.
- Add secure parser defaults with immutable resource limits.
- Parse Invoice and CreditNote directly into the semantic model.
- Add selective projections for routing, attachment extraction, and bookkeeping intake.
- Report unknown, misplaced, duplicate, and obsolete content precisely.
- Test entity expansion, deep nesting, oversized text, malformed UTF-8, duplicate elements, and attachment limits.
- Differential-test model values against the current JAXB-based ReAI parser.

Exit gate: semantic parity on the production-derived corpus, explicit limit behavior, and lower allocation than DOM → JAXB → application-model parsing.

## Phase 4 — SBDH and document metadata

- Add typed sender, receiver, document type, process, instance, and creation metadata.
- Stream SBDH wrapping and unwrapping.
- Generate current SBDH compliance checks from official rules.
- Verify envelope/document sender and receiver consistency.
- Keep envelope support independent of AS4.

Exit gate: current official envelope fixtures and negative cases match reference validation.

## Phase 5 — discovery and reporting

- Add a narrow asynchronous SMP client using JDK HTTP APIs.
- Implement DNS discovery, redirect limits, cache semantics, activation/expiration offsets, certificate parsing, and typed trust results.
- Add EUSR and TSR semantic models and direct writers.
- Keep network policy, caching, and trust injection explicit.
- Differential-test SMP behavior against `peppol-commons`.

Exit gate: protocol fixtures cover redirects, time zones, expiry, malformed metadata, DNS failures, trust failures, and cancellation.

## Phase 6 — transport boundary

- Publish a transport-neutral send/receive API using only Brev types.
- Implement Phase4 adapters for client and servlet/server use.
- Map receipts and errors without exposing Phase4 classes.
- Benchmark end-to-end access-point workloads after HTTP connection reuse and upstream dependency improvements.

Exit gate: ReAI can switch its document layer to Brev while retaining Phase4 transport and can fall back without changing domain code.

## Native AS4 decision gate

A native Peppol-only AS4 implementation is a separate project decision, not a presumed phase. Start it only when all conditions hold:

1. document, validation, reader, and envelope layers are production-proven;
2. measurement shows a material remaining Phase4/WSS4J cost;
3. the cost cannot reasonably be fixed upstream;
4. scope is restricted to the current Peppol AS4 profile and algorithms;
5. official conformance infrastructure and dual-run comparison are available;
6. security ownership, certificate rollover, replay prevention, MIME handling, receipts, and incident response are staffed;
7. the Phase4 adapter remains as an independent reference and fallback.

## Adoption in ReAI

Adoption should be incremental:

1. generate Brev XML in shadow mode;
2. compare semantic values and authoritative validation results;
3. record throughput, allocations, startup, and artifact changes;
4. transmit a controlled subset while retaining old generation fallback;
5. migrate incoming parsing after writer conformance is stable;
6. remove JAXB/Digipost dependencies only after the rollback window closes;
7. retain PHIVE reference validation until generated-rule parity has operated successfully in production.
