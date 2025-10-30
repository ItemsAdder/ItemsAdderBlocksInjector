plugins {
    java
    alias(libs.plugins.paperweight)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly(project(":itemsadder-blocks-injector-core"))
    compileOnly(libs.viaversion)
    compileOnly(libs.protocollib)
    compileOnly(libs.itemsadder)
}
