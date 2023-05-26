package ca.cutterslade.kotlingrpcxds.server

import io.github.oshai.KotlinLogging
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.xds.XdsServerBuilder
import io.grpc.xds.XdsServerCredentials
import kotlinx.coroutines.delay
import org.slf4j.bridge.SLF4JBridgeHandler
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}
private val health = HealthStatusManager()
private fun initJavaLogging() {
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()
}

suspend fun main() {
  try {
    initJavaLogging()

    val healthServer = startHealthServer()

    logger.info { "Health server listening on port ${healthServer.port}" }

    val greeterServer = getGreeterServer()

    logger.info { "Greeter server listening on port ${greeterServer.port}" }

    delay(Duration.INFINITE)
  } catch (e: Exception) {
    logger.error(e) { "Failed to start servers" }
    throw e
  }
}

private fun startHealthServer(): Server =
  ServerBuilder.forPort(8080)
    .addService(health.healthService)
    .build()
    .start()

private fun getGreeterServer(): Server =
  XdsServerBuilder.forPort(
    8443,
    XdsServerCredentials.create(InsecureServerCredentials.create())
  )
    .addService(GreeterServer())
    .addService(ProtoReflectionService.newInstance())
    .addService(health.healthService)
    .intercept(LoggingInterceptor)
    .build()
    .start()
