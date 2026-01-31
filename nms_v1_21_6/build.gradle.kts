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
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
    compileOnly(project(":itemsadder-blocks-injector-core"))
    compileOnly(libs.protocollib)
    compileOnly(libs.itemsadder)
}
