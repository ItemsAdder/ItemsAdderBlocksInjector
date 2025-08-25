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
  ":itemsadder-blocks-injector-jar",
  ":nms_v1_18_R2",
  ":nms_v1_19_R1",
  ":nms_v1_19_R2",
  ":nms_v1_19_R3",
  ":nms_v1_20_R1",
  ":nms_v1_20_R2",
  ":nms_v1_20_R3",
  ":nms_v1_20_6",
  ":nms_v1_21_1",
  ":nms_v1_21_4",
  ":nms_v1_21_5",
  ":nms_v1_21_6",
  ":nms_v1_21_7",
  ":nms_v1_21_8"
)