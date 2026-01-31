import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.0.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

// Depend on all submodules to package their classes into the final jar
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    implementation(project(":itemsadder-blocks-injector-core"))
    implementation(project(":nms_v1_21_11"))
    implementation(project(":nms_v1_21_9"))
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("itemsadder-blocks-injector")
    archiveClassifier.set("")

    exclude("com/volmit/iris/util/data/B.class")
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}

tasks {
    runServer {
        minecraftVersion("1.21.10")
    }
}
