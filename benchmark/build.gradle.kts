plugins {
    java
    id("me.champeau.jmh")
}

dependencies {
    implementation(project(":brev-documents"))
    implementation("no.digipost:peppol-bis-billing-3-generator-api:v0.5-billing-3.0.18")
    testImplementation("com.helger.phive.rules:phive-rules-peppol:4.5.3")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.9")
}
