# Contributing

Contributions should preserve Brev's narrow current-release scope, immutable API, deterministic output, and zero-runtime-dependency document core.

Before submitting a change:

```shell
./gradlew clean build publishToMavenLocal
./gradlew :benchmark:jmhJar
```

Changes to generated XML require independent current-release validation evidence and focused fixtures. Performance changes require JMH measurements with JDK, hardware, command, throughput, and allocation results. Dependency additions require an architectural rationale and must not affect published module runtime classpaths.

Do not include real customer invoices, personal data, certificates, private keys, or production endpoint credentials in issues or fixtures.
