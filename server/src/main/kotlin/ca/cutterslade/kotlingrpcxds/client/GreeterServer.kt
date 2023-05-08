package ca.cutterslade.kotlingrpcxds.client

import ca.cutterslade.kotlingrpcxds.hello.GreeterGrpcKt
import ca.cutterslade.kotlingrpcxds.hello.HelloRequest
import ca.cutterslade.kotlingrpcxds.hello.helloReply
import io.github.oshai.KotlinLogging
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

class GreeterServer : GreeterGrpcKt.GreeterCoroutineImplBase() {
  private companion object {
    val hostName: String = InetAddress.getLocalHost().hostName
  }

  private val counter = AtomicLong()

  override suspend fun sayHello(request: HelloRequest) = helloReply {
    message = "Hello ${request.name}, I'm $hostName"
    host = hostName
  }.also { logger.info { "Served ${counter.incrementAndGet()} requests" } }
}
