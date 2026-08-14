# Validation compiler plan

## Source of truth

Brev will consume the exact official XSD, Schematron, code-list, and country-rule artefacts for its target release. A checked manifest records source URL, upstream version, publication date, license, SHA-256, and local generated outputs.

Artefacts are vendored only when their licenses allow redistribution. Otherwise the release build obtains them from the authoritative location and verifies pinned checksums. Consumer builds never make network requests.

## Compiler pipeline

1. Parse XML securely with external resolution disabled.
2. Resolve Schematron includes inside the pinned artefact set.
3. Select the official phase and expand abstract patterns.
4. Parse XPath expressions into a typed intermediate representation.
5. Resolve namespace prefixes, variables, parameters, node cardinality, and scalar conversions.
6. Reject unsupported functions, axes, coercions, or dynamic behavior.
7. Generate Java rules over semantic business terms or bounded collected input values.
8. Compile generated sources with warnings as errors.
9. run parity tests against PHIVE/Saxon and record the matrix.

## Rule result

Every result must contain:

- official rule ID;
- severity;
- current artefact release;
- human-readable message;
- business-term and UBL location when available;
- stable machine-readable values involved in the failure;
- execution duration only in optional diagnostic mode.

Validation order is deterministic. A caller may request fail-fast behavior, but the default returns all applicable diagnostics.

## Parity corpus

The corpus combines:

- official valid and invalid examples;
- official Schematron unit tests where supplied;
- generated one-rule boundary cases;
- sanitized ReAI output and incoming EHF;
- mutations of identifiers, cardinalities, dates, decimals, VAT groups, totals, countries, and payment data;
- randomized valid typed documents;
- hostile XML parser inputs for the future reader.

Expected results are produced independently by the pinned PHIVE/Saxon reference path. A difference is a build failure until classified and resolved; no allow-list may suppress a fatal-rule mismatch.

## Model validation and XML validation

Typed model construction prevents many invalid states but does not replace official validation. Brev exposes two related operations:

- model validation before serialization, avoiding XML construction and XPath;
- current-profile XML validation for received documents.

Both must produce equivalent official diagnostics for equivalent data. XSD-only structural errors remain specific to received XML because the writer cannot emit unsupported structure.
