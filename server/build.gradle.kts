import ca.cutterslade.kotlingrpcxds.gradle.Versions
import com.google.protobuf.gradle.id

plugins {
  kotlin("jvm")
  id("com.google.protobuf")
  id("com.google.cloud.tools.jib")
}

dependencies {
  protobuf(project(":proto"))

  implementation("io.grpc:grpc-stub:${Versions.grpc}")
  implementation("io.grpc:grpc-protobuf:${Versions.grpc}")
  implementation("com.google.protobuf:protobuf-java-util:${Versions.protobuf}")
  implementation("com.google.protobuf:protobuf-kotlin:${Versions.protobuf}")
  implementation("io.grpc:grpc-kotlin-stub:${Versions.grpcKotlin}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
  implementation("io.grpc:grpc-xds:${Versions.grpc}")
  implementation("io.github.oshai:kotlin-logging-jvm:${Versions.kotlinLogging}")
  implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
  implementation("org.slf4j:jul-to-slf4j:${Versions.slf4j}")

  runtimeOnly("io.grpc:grpc-netty-shaded:${Versions.grpc}")
  runtimeOnly("ch.qos.logback:logback-classic:${Versions.logback}")

  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(17)
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${Versions.protobuf}"
  }
  plugins {
    create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}" }
    create("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.grpcKotlin}:jdk8@jar" }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        create("grpc")
        create("grpckt")
      }
      it.builtins {
        id("kotlin")
      }
    }
  }
}

jib {
  to {
    image = "${properties["image.name.prefix"]}server"
  }
}
