import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar

plugins {
    base
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("me.champeau.jmh") version "0.7.3" apply false
}

val releaseVersion = "0.1.1"
val publishedModules = setOf("brev-core", "brev-documents", "brev-smp", "brev-ap")
val moduleDescriptions = mapOf(
    "brev-core" to "Dependency-free identifiers, codes, and release metadata for current Peppol profiles",
    "brev-documents" to "Current Peppol Billing documents: immutable model, direct UTF-8 writer, and bounded reader",
    "brev-smp" to "Typed Peppol SMP lookup model. Network client is added in a later module release",
    "brev-ap" to "Typed Peppol access-point send/receive model. Transport adapter is added in a later module release"
)

allprojects {
    group = "no.beint.brev"
    version = releaseVersion
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(26))
        }

        dependencies {
            add("testImplementation", platform("org.junit:junit-bom:6.0.3"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(26)
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
        }
    }

    if (name in publishedModules) {
        apply(plugin = "java-library")
        apply(plugin = "com.vanniktech.maven.publish")

        plugins.withId("java") {
            val moduleName = name
            val externalRuntimeArtifacts = configurations.getByName("runtimeClasspath")
                .incoming.artifactView {
                    componentFilter { it is ModuleComponentIdentifier }
                }.files
            val verifyNoRuntimeDependencies = tasks.register("verifyNoRuntimeDependencies") {
                group = "verification"
                description = "Fails when a production module has a third-party runtime dependency"
                inputs.files(externalRuntimeArtifacts)
                doLast {
                    val externalFiles = inputs.files.files
                    check(externalFiles.isEmpty()) {
                        "$moduleName must have zero third-party runtime dependencies: ${externalFiles.joinToString()}"
                    }
                }
            }

            tasks.named("check") {
                dependsOn(verifyNoRuntimeDependencies)
            }

            tasks.withType<Jar>().configureEach {
                from(rootProject.files("NOTICE", "LICENSE")) {
                    into("META-INF")
                }
                manifest {
                    attributes(
                        "Implementation-Title" to project.name,
                        "Implementation-Version" to project.version
                    )
                }
            }
        }

        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            configure(
                JavaLibrary(
                    javadocJar = JavadocJar.Javadoc(),
                    sourcesJar = SourcesJar.Sources()
                )
            )
            publishToMavenCentral()
            if (hasInMemorySigningKey()) {
                signAllPublications()
            }
            pom {
                name.set(project.name)
                description.set(moduleDescriptions.getValue(project.name))
                inceptionYear.set("2026")
                url.set("https://github.com/beint-no/brev")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("beint-no")
                        name.set("Beint")
                        url.set("https://github.com/beint-no")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/beint-no/brev.git")
                    developerConnection.set("scm:git:ssh://git@github.com/beint-no/brev.git")
                    url.set("https://github.com/beint-no/brev")
                }
            }
        }
    }
}

fun Project.hasInMemorySigningKey(): Boolean {
    return providers.gradleProperty("signingInMemoryKey").orNull?.isNotBlank() == true
        || System.getenv("SIGNING_IN_MEMORY_KEY")?.isNotBlank() == true
        || System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")?.isNotBlank() == true
}

tasks.named("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("printReleaseVersion") {
    val release = releaseVersion
    doLast { println(release) }
}
