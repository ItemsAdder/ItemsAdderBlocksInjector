pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
  }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "ItemsAdderBlocksInjector"

include(
  ":itemsadder-blocks-injector-core",
  ":itemsadder-blocks-injector-plugin",
  ":nms_v1_21_11",
  ":nms_v1_21_9",
  ":nms_v1_21_7",
  ":nms_v1_21_6",
  ":nms_v1_21_5",
  ":nms_v1_21_4",
  ":nms_v1_21_3"
)