import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

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
    implementation(project(":nms_v1_21_7"))
    implementation(project(":nms_v1_21_6"))
    implementation(project(":nms_v1_21_5"))
    implementation(project(":nms_v1_21_4"))
    implementation(project(":nms_v1_21_3"))
    implementation(project(":nms_v1_21"))
    implementation(project(":nms_v1_20_6"))
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
        minecraftVersion("1.21.1")
    }
}

listOf(
    "1.21.11",
    "1.21.10",
    "1.21.8",
    "1.21.5",
    "1.21.4",
    "1.21.1",
    "1.20.6"
).forEach {
    registerPaperTask(it)
}

fun registerPaperTask(
    version: String
) {
    listOf(version).forEach { taskName ->
        tasks.register(taskName, RunServer::class) {
            pluginJars.from(tasks.shadowJar.flatMap { it.archiveFile })
            pluginJars.from(
                fileTree("${project.projectDir}/runPaper") {
                    include("*.jar")
                }
            )
            group = "runPaper"
            minecraftVersion(version)

            runDirectory = layout.projectDirectory.dir("${project.projectDir}/runPaper/${version.replace("\\.", "")}")
            systemProperties["com.mojang.eula.agree"] = true
        }
    }
}