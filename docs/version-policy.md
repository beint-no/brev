# Version and compatibility policy

## One active target

Brev packages exactly one supported Peppol release target. The repository may add the next officially published release as an explicit preview before its mandatory date. Once that release becomes Brev's target and the migration window ends, the previous generated rules and code lists are removed.

The initial target is Peppol BIS Billing 3.0.21 with EN 16931 validation artefacts 1.3.16, published 20 May 2026 and mandatory from 17 August 2026.

## What “version” means

Peppol Billing micro releases generally keep UBL 2.1 syntax while changing business rules, country rules, code lists, identifiers, examples, or validation artefacts. Brev therefore separates:

- a stable semantic business model where the specification permits it;
- generated current code-list data;
- release metadata and artefact provenance;
- generated validators tied to an exact release.

The public model changes only when correctness, type safety, or the current semantic contract requires it. Brev does not retain deprecated constructors or adapters to soften those changes.

## Historical documents

The future reader may structurally inspect recognizable older UBL documents. It must label their declared profile as unsupported and must not imply current conformance.

Historical legal or audit validation belongs in a separate archival tool or reference-validator deployment. It is not a dependency of the fast document core.

## Library compatibility

Before `1.0`, any API may change. After `1.0`, semantic versioning describes Brev API compatibility, not support for obsolete Peppol rules:

- a Peppol rule-data refresh that preserves the API may be a minor release;
- a changed semantic model may intentionally require a Brev major release;
- security and conformance corrections may remove behavior immediately.

Applications must record the Brev version and `PeppolRelease` metadata used to create or validate each document.

## Preview releases

Preview support must be opt-in and clearly marked in diagnostics and metadata. Preview output must never be selected implicitly based on the wall clock. Moving preview to current is an explicit library release.
