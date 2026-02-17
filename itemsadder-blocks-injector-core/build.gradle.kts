plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    compileOnly(libs.protocollib)
    compileOnly(libs.itemsadder)
}

// Ensure resources like plugin.yml are included
tasks.processResources {
    filteringCharset = "UTF-8"
}
