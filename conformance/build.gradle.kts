plugins {
    java
}

dependencies {
    testImplementation(project(":brev-documents"))
    testImplementation("com.helger.phive.rules:phive-rules-peppol:4.5.4")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.9")
}
