plugins {
    java
}

dependencies {
    testImplementation(project(":brev-documents"))
    testImplementation("com.helger.phive.rules:phive-rules-peppol:4.5.3")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.9")
}
