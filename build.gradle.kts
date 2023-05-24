import ca.cutterslade.kotlingrpcxds.gradle.Versions

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
      val prefixKey = "image.name.prefix"
      val prefix = project.provider { properties["image.name.prefix"] }
      inputs.properties(prefixKey to prefix)
      val digestFile = "${project.buildDir}/jib-image.digest"
      inputs.file(digestFile)
      val outputFile = "${project.buildDir}/kustomize/kustomization.yaml"
      outputs.file(outputFile)
      doLast {
        val digest = file("${project.buildDir}/jib-image.digest").readText()
        val content = """
        |resources:
        |  - ../../../manifests/${project.name}
        |images:
        |  - name: quay.io/wfhartford/kotlin-grpc-xds/${project.name}
        |    newName: ${prefix.get()}${project.name}
        |    digest: $digest
      """.trimMargin()
        file(outputFile).writeText(content)
      }
    }

    tasks.register("prepareDeployment") {
      dependsOn("build")
      dependsOn("jib")
      dependsOn(kustomizeImageTask)
    }

    configurations.all {
      resolutionStrategy.eachDependency {
        if (requested.group == "commons-logging" && requested.name == "commons-logging")
          useTarget("org.slf4j:jcl-over-slf4j:${Versions.slf4j}")
      }
    }
  }
}

tasks.named("jib") {
  enabled = false
}
