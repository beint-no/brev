# Performance baseline

These numbers are a development baseline, not a production migration claim. The comparison measures one small positive invoice written to a returned byte array by Brev and by the Digipost generator previously used in ReAI. Compatibility tests compare every semantic XML leaf and attribute for ReAI-shaped invoices and validate both outputs against the current Peppol rules.

## 2026-08-30 Digipost comparison

- Hardware: Apple M5 Pro, arm64.
- JVM: OpenJDK 26.0.2.1.
- JMH: 1.36.
- Workload: serialization from preconstructed, semantically equivalent one-line invoice models.
- Warmup: 3 × 1 second.
- Measurement: 5 × 1 second.
- Forks: 2.
- GC profiler enabled.

Command:

```shell
java -jar benchmark/build/libs/benchmark-0.1.1-jmh.jar \
  '.*BillingWriterBenchmark.*' -wi 3 -i 5 -f 2 -w 1s -r 1s -prof gc
```

| Generator | Throughput | Allocation |
|---|---:|---:|
| Brev | 263,003 ops/s | 17,568 B/op |
| Digipost | 38,267 ops/s | 272,016 B/op |

For this workload, Brev delivered **6.87× the throughput** and used **15.48× less allocation per operation**. The benchmark returns the XML byte array in both cases. The Digipost path mirrors ReAI's `PeppolBillingApi.create(document).inputStream().readAllBytes()` behavior.

The production artifact envelope for the measured paths is also smaller:

| Generator | Runtime JARs | Classes | Compressed size |
|---|---:|---:|---:|
| Brev | 2 | 45 | 89,693 bytes |
| Digipost + Eaxy + JSR-305 | 4 | 192 | 233,648 bytes |

Brev removes two runtime artifacts, 147 classes, and 143,955 compressed bytes from this path. Its production modules retain zero third-party runtime dependencies.

This clears the repository's initial 5× throughput and 3× allocation gates for the measured invoice. The compatibility suite covers invoices, credit notes, discounts, every VAT category ReAI emits, rounding, BIC details, references, and attachments. Both implementations produce the same semantic content and pass the current PHIVE rules in all eight scenarios.

## 2026-08-14 smoke test

- Hardware: Apple M5 Pro, arm64.
- JVM: OpenJDK 26.0.2.
- JMH: 1.36.
- Benchmark: `BillingWriterBenchmark.writeInvoice`.
- Warmup: 2 × 1 second.
- Measurement: 3 × 1 second.
- Forks: 1.

Command:

```shell
java -jar benchmark/build/libs/benchmark-0.1.0-SNAPSHOT-jmh.jar \
  '.*BillingWriterBenchmark.*' -wi 2 -i 3 -f 1 -w 1s -r 1s
```

Observed throughput: **421,555 operations/second** average, with individual iterations from 418,711 to 424,416 operations/second.

A shorter GC-profiler run reported **17,776 bytes allocated per operation**. Most of that first baseline is intentionally visible overhead from a new 8 KiB writer buffer, the growing in-memory destination, the returned byte array, decimal formatting, and derived-value objects. The direct `OutputStream` API avoids the convenience method's final byte-array copy. Buffer reuse and allocation work should be driven by comparative profiles rather than the smoke number alone.

## Artifact size

The current 0.1.1 production JARs are:

| Artifact | Compressed size |
|---|---:|
| `brev-core` | 15,778 bytes |
| `brev-documents` | 73,915 bytes |
| Combined | 89,693 bytes |

Both production modules have zero third-party runtime dependencies. The test-only conformance and benchmark modules deliberately carry PHIVE/Saxon/JAXB and JMH respectively.

## Remaining comparison work

Before claiming a speedup, add benchmark cases for:

1. Digipost and Brev object construction plus serialization;
2. direct streaming to a pre-sized or black-hole output destination;
3. typical, large-line-count, credit-note, discount, multi-VAT, and attachment-heavy invoices;
4. cold startup and first-document latency;
5. retained memory under the ReAI workload;
6. shadow comparison over a representative production invoice corpus.

The benchmark input and resulting XML must be semantically equivalent and independently validated.
