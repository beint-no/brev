# Publishing

Brev publishes `brev-core`, `brev-documents`, `brev-smp`, and `brev-ap` to
Maven Central under `no.beint.brev`.

## Secrets

Do not put tokens or the private signing key in the repository. Set these
environment variables in the shell or CI environment:

```text
MAVEN_CENTRAL_USERNAME
MAVEN_CENTRAL_PASSWORD
SIGNING_IN_MEMORY_KEY
SIGNING_IN_MEMORY_KEY_ID
SIGNING_IN_MEMORY_KEY_PASSWORD
```

Generate a Central Portal user token at
<https://central.sonatype.com/usertoken>.

## Release

Set `releaseVersion` in `build.gradle.kts`, merge the change to `main`, and push
a matching tag:

```shell
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions verifies that the tag matches the Gradle version, then builds
and publishes that exact tagged commit. No local Maven credentials are needed.
