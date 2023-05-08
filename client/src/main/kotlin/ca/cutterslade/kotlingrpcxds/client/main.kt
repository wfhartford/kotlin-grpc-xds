package ca.cutterslade.kotlingrpcxds.client

import ca.cutterslade.kotlingrpcxds.hello.GreeterGrpcKt
import ca.cutterslade.kotlingrpcxds.hello.helloRequest
import io.github.oshai.KotlinLogging
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.xds.XdsChannelCredentials
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import org.slf4j.bridge.SLF4JBridgeHandler
import java.net.InetAddress
import kotlin.time.Duration.Companion.milliseconds

private val hostName: String = InetAddress.getLocalHost().hostName
private val targetUrl: String = System.getenv("TARGET_URL") ?: "xds:///localhost:8443"
private val logger = KotlinLogging.logger {}
suspend fun main() {
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  val channel =
    Grpc.newChannelBuilder(targetUrl, XdsChannelCredentials.create(InsecureChannelCredentials.create())).build()

  val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

  flow {
    while (true) {
      emit(
        stub.sayHello(
          helloRequest {
            name = hostName
          }
        )
      )
      delay(500.milliseconds)
    }
  }
    .map { it.host }
    .onEach {
      logger.info { "Received response from $it" }
    }
    .runningFold(emptyMap<String, Int>()) { acc, host ->
      acc + (host to (acc[host]?.let { it + 1 } ?: 1))
    }
    .collect {
      logger.info {
        buildString {
          append("Received count per host:\n")
          it.forEach {(host, count) ->
            append("  $host: $count\n")
          }
        }
      }
    }
}
