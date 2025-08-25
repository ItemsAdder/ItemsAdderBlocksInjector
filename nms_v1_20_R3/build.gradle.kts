plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    compileOnly(project(":itemsadder-blocks-injector-core"))
    compileOnly("com.viaversion:viaversion-api:5.5.0-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("beer.devs:FastNbt-jar:1.4.14")
}
