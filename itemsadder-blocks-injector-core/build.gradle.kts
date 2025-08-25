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

    compileOnly("com.viaversion:viaversion-api:5.5.0-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("beer.devs:FastNbt-jar:1.4.14")
}

// Ensure resources like plugin.yml are included
tasks.processResources {
    filteringCharset = "UTF-8"
}
