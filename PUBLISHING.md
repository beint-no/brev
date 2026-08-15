# Publishing

Brev publishes `brev-core`, `brev-documents`, `brev-smp`, and `brev-ap` to
Maven Central under `no.beint.brev`.

## Secrets

Do not put tokens or the private signing key in the repository. Export them as
environment variables (see `~/.config/skald/maven-central.env` on a release
machine; the same Central namespace and signing key are used):

```text
MAVEN_CENTRAL_USERNAME
MAVEN_CENTRAL_PASSWORD
SIGNING_IN_MEMORY_KEY
SIGNING_IN_MEMORY_KEY_ID
SIGNING_IN_MEMORY_KEY_PASSWORD
```

Gradle also accepts the same values as `ORG_GRADLE_PROJECT_mavenCentralUsername`,
`ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`,
`ORG_GRADLE_PROJECT_signingInMemoryKeyId`, and
`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`.

Generate a Central Portal user token at
<https://central.sonatype.com/usertoken>.

## Release

Set `releaseVersion` in `build.gradle.kts`, then:

```shell
./gradlew clean build
./gradlew publishAndReleaseToMavenCentral
```

The Central Portal typically takes 10–30 minutes after a successful deployment
before the artifacts are downloadable from Maven Central.

For a CI release, push a matching tag:

```shell
git tag v0.1.0
git push origin v0.1.0
```
