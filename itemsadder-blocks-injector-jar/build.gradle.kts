import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.0.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18" apply false
}

// Depend on all submodules to package their classes into the final jar
dependencies {
    implementation(project(":itemsadder-blocks-injector-core"))
    implementation(project(":nms_v1_21_8"))
    implementation(project(":nms_v1_21_7"))
    implementation(project(":nms_v1_21_6"))
    implementation(project(":nms_v1_21_5"))
    implementation(project(":nms_v1_21_4"))
    implementation(project(":nms_v1_21_1"))
    implementation(project(":nms_v1_20_6"))
    implementation(project(":nms_v1_20_R3"))
    implementation(project(":nms_v1_20_R2"))
    implementation(project(":nms_v1_20_R1"))
    implementation(project(":nms_v1_19_R3"))
    implementation(project(":nms_v1_19_R2"))
    implementation(project(":nms_v1_19_R1"))
    implementation(project(":nms_v1_18_R2"))
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("itemsadder-blocks-injector")
    archiveClassifier.set("")

    exclude("com/volmit/iris/util/data/B.class")

    relocate("com.alessiodp", "dev.lone.blocksinjector.shaded.com.alessiodp")
    relocate("org.jetbrains", "dev.lone.blocksinjector.shaded.org.jetbrains")
    relocate("org.intellij", "dev.lone.blocksinjector.shaded.org.intellij")
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}

tasks {
    runServer {
        minecraftVersion("1.21.8")
    }
}
