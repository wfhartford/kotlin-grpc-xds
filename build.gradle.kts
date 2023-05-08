plugins {
  kotlin("jvm") version "1.8.21"
  id("idea")
  id("com.google.protobuf") version "0.9.3"
  id("com.google.cloud.tools.jib") version "3.3.1"
}

allprojects {
  group = "ca.cutterslade.kotlingrpcxds"
  version = "1.0-SNAPSHOT"
  repositories {
    mavenCentral()
  }
}

subprojects {
  if (name in setOf("client", "server")) {
    val kustomizeImageTask = tasks.register("kustomizeImage") {
      dependsOn("jib")
      inputs.properties("image.name.prefix" to properties["image.name.prefix"])
      inputs.file("${project.buildDir}/jib-image.digest")
      outputs.file("${project.buildDir}/kustomize/kustomization.yaml")
      doLast {
        val digest = file("${project.buildDir}/jib-image.digest").readText()
        val content = """
        |resources:
        |  - ../../src/main/k8s
        |images:
        |  - name: ${project.name}
        |    newName: ${properties["image.name.prefix"]}${project.name}
        |    digest: $digest
      """.trimMargin()
        file("${project.buildDir}/kustomize/kustomization.yaml").writeText(content)
      }
    }

    tasks.register("prepareDeployment") {
      dependsOn("build")
      dependsOn("jib")
      dependsOn(kustomizeImageTask)
    }
  }
}

tasks.named("jib") {
  enabled = false
}
