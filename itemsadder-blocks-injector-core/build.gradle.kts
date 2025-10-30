plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("com.alessiodp.libby:libby-bukkit:2.0.0-SNAPSHOT")

    compileOnly(libs.viaversion)
    compileOnly(libs.protocollib)
    compileOnly(libs.itemsadder)
}

// Ensure resources like plugin.yml are included
tasks.processResources {
    filteringCharset = "UTF-8"
}
