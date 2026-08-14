plugins {
    java
}

dependencies {
    testImplementation(project(":brev-billing"))
    testImplementation("com.helger.phive.rules:phive-rules-peppol:4.4.1")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.9")
}
