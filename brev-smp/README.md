# brev-smp

Typed Peppol SMP model for the Brev modular library.

This module publishes identifier and metadata types so applications can depend on
a stable API without pulling Phase4, JAXB, or an HTTP stack. A JDK-only SMP
client belongs in a later release of this same module, not a second repository.

Do not take this artifact if you only generate or parse Billing documents.
Use `brev-documents`.
