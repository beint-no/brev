# Performance baseline

These numbers are a development smoke test, not a cross-library performance claim. The benchmark currently measures one small positive invoice written through the convenience `toByteArray` path. A publishable comparison requires the same semantic invoice, output destination, JVM settings, and validation policy for Brev and the existing ReAI generator.

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

The initial production JARs are:

| Artifact | Compressed size |
|---|---:|
| `brev-core` | 15,653 bytes |
| `brev-billing` | 29,733 bytes |
| Combined | 45,386 bytes |

Both production modules have zero third-party runtime dependencies. The test-only conformance and benchmark modules deliberately carry PHIVE/Saxon/JAXB and JMH respectively.

## Required comparison

Before claiming a speedup, add benchmark cases for:

1. Digipost object construction plus `PeppolBillingApi.create(...).inputStream().readBytes()`;
2. Brev model construction plus `toByteArray()`;
3. serialization alone from preconstructed models;
4. direct streaming to a pre-sized or black-hole output destination;
5. small, typical, large-line-count, and attachment-heavy invoices;
6. cold startup and first-document latency;
7. throughput, bytes allocated per operation, retained memory, and artifact/class count.

The benchmark input and resulting XML must be semantically equivalent and independently validated.
